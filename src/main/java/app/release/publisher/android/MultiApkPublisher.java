package app.release.publisher.android;

import app.release.model.CommandLineArguments;
import app.release.model.Configuration;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Charsets;
import com.google.api.services.androidpublisher.model.CountryTargeting;
import com.google.api.services.androidpublisher.model.LocalizedText;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


/**
 * Batch uploads multiple apk files to Play Store.
 */
@Slf4j
public class MultiApkPublisher extends ApkPublisher {

    private Configuration configuration = null;
    private Path baseFolder = null;

    public MultiApkPublisher(CommandLineArguments arguments) {
        super(arguments);
    }

    @Override
    public void publish() throws IOException, GeneralSecurityException {
        Path configFile = FileSystems.getDefault().getPath(arguments.getFile()).normalize();
        configuration = JacksonFactory.getDefaultInstance().createJsonParser(
                new InputStreamReader(new FileInputStream(configFile.toFile()), Charsets.UTF_8))
                .parse(Configuration.class);

        baseFolder = configuration.baseFolder.isEmpty() ? configFile.getParent()
                : FileSystems.getDefault().getPath(configuration.baseFolder);
        log.info("ApplicationPublisher base folder is: [{}]", baseFolder);
        log.info("ApplicationPublisher Configuration: [{}]", configuration);


        for (String flavor : configuration.flavors) {
            log.info("ApplicationPublisher checking configured flavor: [{}]", flavor);

            String apkFileName = configuration.apkFilePattern
                    .replace("{version}", configuration.version)
                    .replace("{flavor}", flavor);

            Path apkFile = baseFolder.resolve(apkFileName).normalize();

            String mappingFileName = configuration.mappingFilePattern
                    .replace("{version}", configuration.version)
                    .replace("{flavor}", flavor);

            Path mappingFile = baseFolder.resolve(mappingFileName).normalize();

            if (!apkFile.toFile().isFile()) {
                log.warn("APK file [{}] not found, skipping...", apkFile);
                continue;
            }
            if (!mappingFile.toFile().isFile()) {
                log.warn("Mapping file [{}] not found, skipping...", mappingFile);
                continue;
            }

            CountryTargeting countryTargeting = getCountryTargeting(flavor);

            ArrayList<LocalizedText> releaseNotes = getReleaseNotes(flavor);

            if (!configuration.unattended) {
                if (!confirm("Publish %s\nMapping:%s\nCountries: %s\nRelease notes: %s\n\n(Y/N)?",
                        apkFile, mappingFile, countryTargeting != null ? countryTargeting.getCountries().toString() : "",
                        releaseNotes.toString())) {
                    log.info("Skipped [{}]", flavor);
                    continue;
                }
            }
            try {
                publishSingleApk(apkFile, mappingFile, countryTargeting, releaseNotes, configuration.track, configuration.status);
            } catch (Exception e) {
                if (configuration.abortOnError) throw e;
            }
        }
    }

    private ArrayList<LocalizedText> getReleaseNotes(String flavor) throws IOException {
        String[] locales = configuration.locales.get(flavor);

        String releaseNotesFile = configuration.releaseNotesByFlavor.get(flavor);
        Path releaseNotesPath = baseFolder.resolve(releaseNotesFile != null ? releaseNotesFile :
                configuration.releaseNotes).normalize();

        ArrayList<LocalizedText> releaseNotes = new ArrayList<>(getReleaseNotesFromJson(releaseNotesPath));
        releaseNotes.removeIf(localizedText -> !Arrays.asList(locales).contains(localizedText.getLanguage()));

        log.info("ApplicationPublisher release notes: [{}]", releaseNotes);
        return releaseNotes;
    }

    private CountryTargeting getCountryTargeting(String flavor) {
        String[] flavorCountries = configuration.countriesByFlavor.get(flavor);

        List<String> countries = Arrays.asList(
                flavorCountries != null ? flavorCountries : configuration.countries
        );

        CountryTargeting countryTargeting = countries.isEmpty() ? null :
                new CountryTargeting().setCountries(countries).setIncludeRestOfWorld(false);

        if (countryTargeting != null)
            log.info("ApplicationPublisher Effective country list is: [{}]", countryTargeting.getCountries());
        return countryTargeting;
    }

    private boolean confirm(String fmt, Object... args) {
        while (true) {
            System.out.printf(fmt, args);

            try {
                //noinspection ResultOfMethodCallIgnored
                System.in.skip(System.in.available());
                String answer = String.valueOf((char) System.in.read()).toUpperCase(Locale.ROOT);

                if (answer.equals("N")) {
                    return false;
                }
                if (!answer.equals("Y")) {
                    return true;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}