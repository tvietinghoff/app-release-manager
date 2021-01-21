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
                "  apkFilePattern='" + apkFilePattern + "'\n" +
                "  mappingFilePattern='" + mappingFilePattern + "'\n" +
                "  countries=" + Arrays.toString(countries) + '\n' +
                "  countriesByFlavor=" + countriesByFlavor + '\n' +
                '}';
    }

    @Key
    public String track = "alpha";
    @Key
    public String[] flavors = {};
    @Key
    public String version = "";
    @Key
    public Map<String, String> releaseNotesByFlavor = new ArrayMap<>();
    @Key
    public String releaseNotes = "";
    @Key
    public String baseFolder = null;
    @Key
    public String apkFilePattern = "de.{flavor}.app-{version}.apk";
    @Key
    public String mappingFilePattern = "de.{flavor}.app-{version}-mapping.txt";
    @Key
    public String[] countries = {};
    @Key
    public Map<String, String[]> countriesByFlavor = new ArrayMap<>();
    @Key
    public boolean unattended;
    @Key
    public boolean abortOnError;
}
