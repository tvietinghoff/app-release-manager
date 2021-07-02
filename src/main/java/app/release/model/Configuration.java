package app.release.model;

import com.google.api.client.util.ArrayMap;
import com.google.api.client.util.Key;

import java.util.Arrays;
import java.util.Map;

public class Configuration {

    @Override
    public String toString() {
        return "Configuration {\n" +
                "  track='" + track + "'\n" +
                "  flavors=" + Arrays.toString(flavors) + '\n' +
                "  version='" + version + "'\n" +
                "  releaseNotesByFlavor=" + releaseNotesByFlavor + '\n' +
                "  releaseNotes='" + releaseNotes + "'\n" +
                "  baseFolder='" + baseFolder + "'\n" +
                "  apkFilePattern='" + appFilePattern + "'\n" +
                "  mappingFilePattern='" + mappingFilePattern + "'\n" +
                "  countries=" + Arrays.toString(countries) + '\n' +
                "  countriesByFlavor=" + countriesByFlavor + '\n' +
                "  locales=" + locales + '\n' +
                '}';
    }

    @Key
    public String track = "alpha";
    @Key
    public String[] flavors = {};
    @Key
    public String version = "";
    /**
     * release notes or path to release notes JSON
     */
    @Key
    public String releaseNotes = "";
    /**
     * release notes or path to release notes JSON by flavor
     */
    @Key
    public Map<String, String> releaseNotesByFlavor = new ArrayMap<>();
    /**
     * base folder for all file names
     */
    @Key
    public String baseFolder = null;
    @Key
    public String appFilePattern = "de.{flavor}.app-{version}.{fileType}";
    @Key
    public FileType fileType = FileType.APK;
    @Key
    public String packageNamePattern = "de.{flavor}.app";
    @Key
    public String mappingFilePattern = "de.{flavor}.app-{version}-mapping.txt";
    /**
     * used for country targeting
     */
    @Key
    public String[] countries = {};
    /**
     * used for country targeting
     */
    @Key
    public Map<String, String[]> countriesByFlavor = new ArrayMap<>();
    /**
     * used for release notes lookup
     */
    @Key
    public Map<String, String[]> locales = new ArrayMap<>();
    @Key
    public boolean unattended;
    @Key
    public boolean abortOnError;
    @Key
    public TrackStatus status = TrackStatus.completed;
}
