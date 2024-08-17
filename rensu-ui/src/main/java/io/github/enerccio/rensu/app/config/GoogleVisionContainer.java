package io.github.enerccio.rensu.app.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import org.springframework.beans.factory.DisposableBean;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class GoogleVisionContainer implements DisposableBean {

    private final Map<String, ImageAnnotatorClient> clients = new HashMap<>();

    public ImageAnnotatorClient getClient(String json) {
        return clients.computeIfAbsent(json, this::createClient);
    }

    private ImageAnnotatorClient createClient(String credentials) {
        try {
            GoogleCredentials creds = GoogleCredentials.fromStream(new ByteArrayInputStream(credentials.getBytes(StandardCharsets.UTF_8)));
            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(() -> creds).build();
            return ImageAnnotatorClient.create(settings);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() throws Exception {
        for (ImageAnnotatorClient client : clients.values())
            client.close();
    }
}
