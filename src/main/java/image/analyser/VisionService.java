package image.analyser;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.google.cloud.vision.v1.Feature.Type.*;
import static java.util.Collections.singletonList;

@Service
public class VisionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisionService.class);
    private final ImageAnnotatorClient imageAnnotatorClient;
    private final CloudStorageService cloudStorageService;
    private final String bucketName;
    private final UploadHistoryRepository repository;

    public VisionService(ImageAnnotatorClient imageAnnotatorClient, CloudStorageService cloudStorageService,
                         UploadHistoryRepository uploadHistoryRepository,
                         @Value("${image.analyser.cloud.storage.bucket.name}") String bucketName) {
        this.imageAnnotatorClient = imageAnnotatorClient;
        this.cloudStorageService = cloudStorageService;
        this.bucketName = bucketName;
        repository = uploadHistoryRepository;
    }

    public String analyse(String fileName, byte[] fileContent) {
        LOGGER.debug("Analysing file {}....", fileName);
        BatchAnnotateImagesResponse batchAnnotateImagesResponse = imageAnnotatorClient.batchAnnotateImages(
                singletonList(AnnotateImageRequest.newBuilder()
                        .addFeatures(Feature.newBuilder().setType(FACE_DETECTION).build())
                        .addFeatures(Feature.newBuilder().setType(LANDMARK_DETECTION).build())
                        .addFeatures(Feature.newBuilder().setType(LOGO_DETECTION).build())
                        .addFeatures(Feature.newBuilder().setType(LABEL_DETECTION).build())
                        .addFeatures(Feature.newBuilder().setType(TEXT_DETECTION).build())
                        .addFeatures(Feature.newBuilder().setType(DOCUMENT_TEXT_DETECTION).build())
                        .setImage(Image.newBuilder()
                                .setContent(ByteString.copyFrom(fileContent))
                                .build())
                        .build())
        );
        LOGGER.debug("Analysis completed. Result: {}", batchAnnotateImagesResponse.toString());

        LOGGER.debug("Uploading file {} to {} bucket....", fileName, bucketName);
        cloudStorageService.uploadFile(fileName, fileContent, bucketName);
        LOGGER.debug("Upload completed");

        LOGGER.debug("Saving it to db");
        repository.save(new UploadHistoryEntity("test", fileName, batchAnnotateImagesResponse.toString()));
        LOGGER.debug("Save completed");

        return batchAnnotateImagesResponse.toString();
    }
}
