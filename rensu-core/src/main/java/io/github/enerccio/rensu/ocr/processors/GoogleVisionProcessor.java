package io.github.enerccio.rensu.ocr.processors;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.protobuf.ByteString;
import io.github.enerccio.rensu.ocr.OcrProcessor;
import io.github.enerccio.rensu.ocr.OcrTaskChain;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GoogleVisionProcessor implements OcrProcessor {

    private static final Logger log = LoggerFactory.getLogger(GoogleVisionProcessor.class);
    private final ImageAnnotatorClient client;

    public GoogleVisionProcessor(ImageAnnotatorClient client) {
        this.client = client;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void testCredentials(String json) throws Exception {
        GoogleCredentials creds = GoogleCredentials.fromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(() -> creds).build();
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
            ByteString imgBytes = ByteString.copyFrom(IOUtils.toByteArray(Objects
                    .requireNonNull(GoogleVisionProcessor.class
                            .getResourceAsStream("/gcredtestimage.png"))));
            List<AnnotateImageRequest> requests = new ArrayList<>();
            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
            AnnotateImageRequest request =
                    AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
            requests.add(request);
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            response.getResponsesList();
        }
    }

    @Override
    public void process(long id, byte[] input, OcrTaskChain chain) throws Exception {
        ByteString imgBytes = ByteString.copyFrom(input);
        List<AnnotateImageRequest> requests = new ArrayList<>();
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
        List<AnnotateImageResponse> responses = response.getResponsesList();

        List<String> texts = new ArrayList<>();
        for (AnnotateImageResponse res : responses) {
            if (res.hasError()) {
                log.error("Error: {}", res.getError().getMessage());
                continue;
            }
            for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
                texts.add(annotation.getDescription());
                break;
            }
        }
        chain.finish(id, String.join("", texts));
    }
}
