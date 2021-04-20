package app.release.publisher.android;

import app.release.model.CommandLineArguments;
import app.release.model.TrackStatus;
import app.release.publisher.Publisher;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Charsets;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.api.services.androidpublisher.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.extern.slf4j.Slf4j;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.*;


/**
 * Uploads android apk files to Play Store.
 */
@Slf4j
public class ApkPublisher implements Publisher {

    private static final String MIME_TYPE_MAPPING = "application/octet-stream";
    private static final String MIME_TYPE_APK = "application/vnd.android.package-archive";
    protected final CommandLineArguments arguments;

    public ApkPublisher(CommandLineArguments arguments) {
        this.arguments = arguments;
    }

    @Override
    public void publish() throws IOException, GeneralSecurityException {

        Path apkFile = FileSystems.getDefault().getPath(arguments.getFile()).normalize();
        Path mappingFile = arguments.getMappingFile() == null ? null :
                FileSystems.getDefault().getPath(arguments.getMappingFile()).normalize();

        CountryTargeting countryTargeting = arguments.getCountries() == null ? null :
                new CountryTargeting()
                        .setCountries(Arrays.asList(arguments.getCountries().split(",")))
                        .setIncludeRestOfWorld(false);

        ArrayList<LocalizedText> releaseNotes = new ArrayList<>();
        // load release notes
        log.info("Loading release notes...");
        if (arguments.getNotesPath() != null) {
            Path notesFile = FileSystems.getDefault().getPath(arguments.getNotesPath()).normalize();
            if (arguments.getNotesPath().endsWith(".json")) {
                releaseNotes.addAll(getReleaseNotesFromJson(notesFile));
            } else {
                String notesContent = new String(Files.readAllBytes(notesFile));
                releaseNotes.add(new LocalizedText().setLanguage(Locale.US.toString()).setText(notesContent));
            }
        } else if (arguments.getNotes() != null) {
            releaseNotes.add(new LocalizedText().setLanguage(Locale.US.toString()).setText(arguments.getNotes()));
        }

        publishSingleApk(apkFile, mappingFile, countryTargeting, releaseNotes, arguments.getTrackName(), arguments.getStatus());
    }

    protected Collection<LocalizedText> getReleaseNotesFromJson(Path notesFile) throws IOException {
        return JacksonFactory.getDefaultInstance().createJsonParser(
                new InputStreamReader(new FileInputStream(notesFile.toFile()), Charsets.UTF_8))
                .parseArray(ArrayList.class, LocalizedText.class);
    }

    private GoogleCredentials getGoogleCredentials() throws IOException {
        // load key file credentials
        log.info("Loading account credentials...");
        Path jsonKey = FileSystems.getDefault().getPath(arguments.getJsonKeyPath()).normalize();
        return ServiceAccountCredentials.fromStream(
                new FileInputStream(jsonKey.toFile()))
                .createScoped(Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER));
    }

    protected void publishSingleApk(Path apkFile, Path mappingFile, CountryTargeting countryTargeting, List<LocalizedText> releaseNotes, String trackName, TrackStatus status) throws IOException, GeneralSecurityException {
        // load apk file info
        log.info("Loading apk file information...");

        ApkFile apkInfo = new ApkFile(apkFile.toFile());
        ApkMeta apkMeta = apkInfo.getApkMeta();
        final String applicationName = arguments.getAppName() == null ? apkMeta.getName() : arguments.getAppName();
        final String packageName = apkMeta.getPackageName();
        String versionName = apkMeta.getVersionName();
        log.info("ApplicationPublisher Name: [{}]", apkMeta.getName());
        log.info("ApplicationPublisher Id: [{}]", apkMeta.getPackageName());
        log.info("ApplicationPublisher Version Code: [{}]", apkMeta.getVersionCode());
        log.info("ApplicationPublisher Version Name: [{}]", versionName);
        apkInfo.close();

        // init publisher
        log.info("Initialising publisher service...");
        AndroidPublisher publisher = new AndroidPublisher.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(getGoogleCredentials())
        ).setApplicationName(applicationName).build();


        // create an edit
        log.info("Initialising new edit...");
        AppEdit edit = publisher.edits().insert(packageName, null).execute();
        final String editId = edit.getId();
        log.info("Edit created. Id: [{}]", editId);

        try {
            // publish the apk
            log.info("Uploading apk file...");
            AbstractInputStreamContent apkContent = new FileContent(MIME_TYPE_APK, apkFile.toFile());
            Apk apk = publisher.edits().apks().upload(packageName, editId, apkContent).execute();
            Integer versionCode = apk.getVersionCode();
            log.info("Apk uploaded. Version Code: [{}]", versionCode);

            if (mappingFile != null) {
                log.info("Uploading mapping file...");
                AbstractInputStreamContent mappingContent = new FileContent(MIME_TYPE_MAPPING, mappingFile.toFile());
                DeobfuscationFilesUploadResponse mapping =
                        publisher.edits().deobfuscationfiles().upload(packageName, editId, versionCode, "proguard", mappingContent).execute();
                log.info("Mapping file uploaded. Type: [{}]", mapping.getDeobfuscationFile().getSymbolType());
            }

            // create a release on track
            log.info("Creating a release on track: [{}]", trackName);
            TrackRelease release = new TrackRelease().setName(versionName).setStatus(status.name())
                    .setVersionCodes(Collections.singletonList((long) versionCode))
                    .setCountryTargeting(countryTargeting)
                    .setReleaseNotes(releaseNotes);
            Track track = new Track().setReleases(Collections.singletonList(release)).setTrack(trackName);
            publisher.edits().tracks().update(packageName, editId, trackName, track).execute();
            log.info("Release created on track: [{}]", trackName);

            // commit edit
            log.info("Committing edit...");
            publisher.edits().commit(packageName, editId).execute();
            log.info("Success. Committed Edit id: [{}]", editId);

            // Success
        } catch (Exception e) {
            // error message
            String msg = "Operation Failed: " + e.getMessage();

            // abort
            log.error(msg);
            log.error("Operation failed due to an error!, Deleting edit...");
            try {
                publisher.edits().delete(packageName, editId).execute();
            } catch (Exception e2) {
                // log abort error as well
                msg += "\nFailed to delete edit: " + e2.getMessage();
            }

            // forward error with message
            throw new IOException(msg, e);
        }
    }
}