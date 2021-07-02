package app.release.model;

import com.google.api.client.util.Value;

public enum FileType {

    @Value("APK")
    APK(FileType.MIME_TYPE_APK),
    @Value("AAB")
    AAB(FileType.MIME_TYPE_AAB);

    FileType(String mimeType) {
        this.mimeType = mimeType;
    }

    public static final String MIME_TYPE_AAB = "application/octet-stream";
    public static final String MIME_TYPE_APK = "application/vnd.android.package-archive";

    public final String mimeType;
}
