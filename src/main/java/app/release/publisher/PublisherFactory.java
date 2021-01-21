package app.release.publisher;

import app.release.publisher.android.AabPublisher;
import app.release.publisher.android.ApkPublisher;
import app.release.model.CommandLineArguments;
import app.release.publisher.android.MultiApkPublisher;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PublisherFactory {

    private PublisherFactory() {
    }

    public static Publisher buildPublisher(CommandLineArguments arguments) {
        String fileName = arguments.getFile();
        Publisher publisher;
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".apk")) {
            log.info("Constructing APK Publisher for file [{}]", fileName);
            publisher = new ApkPublisher(arguments);
        } else if (lowerFileName.endsWith(".json")) {
            log.info("Constructing MultiAPK Publisher for file [{}]", fileName);
            publisher = new MultiApkPublisher(arguments);
        } else if (lowerFileName.endsWith(".aab")) {
            log.info("Constructing AAB Publisher for file [{}]", fileName);
            publisher = new AabPublisher(arguments);
        } else {
            log.error("Unsupported File type received: [{}]", fileName);
            throw new RuntimeException("File Type is not supported for: " + fileName);
        }

        return publisher;
    }
}
