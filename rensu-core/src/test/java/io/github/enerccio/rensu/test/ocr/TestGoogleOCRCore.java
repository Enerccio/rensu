package io.github.enerccio.rensu.test.ocr;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import io.github.enerccio.rensu.ocr.RensuOcr;
import io.github.enerccio.rensu.ocr.processors.GoogleVisionProcessor;
import io.github.enerccio.rensu.ocr.processors.StringTrimProcessor;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SuppressWarnings("DataFlowIssue")
@SpringJUnitConfig(locations = "/test-config.xml")
public class TestGoogleOCRCore {
    private static final Logger log = LoggerFactory.getLogger(TestGoogleOCRCore.class);
    private static InputStream cfile;
    private static ImageAnnotatorClient client;
    @Autowired
    private RensuOcr rensu;
    private byte[] data;
    private byte[] data2;
    private Throwable exception;
    private String result;
    private long id;
    private long recId;

    @BeforeAll
    public static void setupAll() throws Exception {
        cfile = TestGoogleOCRCore.class.getResourceAsStream("/googleprivatekey.json");

        GoogleCredentials creds = GoogleCredentials.fromStream(cfile);
        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(() -> creds).build();
        client = ImageAnnotatorClient.create(settings);
    }

    @AfterAll
    public static void closeAll() {
        if (client != null)
            client.close();
    }

    @BeforeEach
    public void setup() throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            InputStream is = TestGoogleOCRCore.class.getResourceAsStream("/test1.png");
            IOUtils.copyLarge(is, bos);
            data = bos.toByteArray();
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            InputStream is = TestGoogleOCRCore.class.getResourceAsStream("/test2.png");
            IOUtils.copyLarge(is, bos);
            data2 = bos.toByteArray();
        }
    }

    @Test
    public void basicOcr() throws Exception {
        if (cfile == null)
            return;

        id = new Random().nextInt();
        rensu.process(id, data, Arrays.asList(new GoogleVisionProcessor(client),
                new StringTrimProcessor()), (id, result, exception) -> {
            this.recId = id;
            this.result = (String) result;
            this.exception = exception;
        });

        if (exception != null)
            log.error(exception.getMessage(), exception);

        assertNull(exception);
        assertEquals(id, recId);
        assertEquals("過去に着用した衣装、 描いた絵画、写真など彼女の思い出を辿るイベントとなります。", result);
    }

    @Test
    public void complexOcr() throws Exception {
        if (cfile == null)
            return;

        id = new Random().nextInt();
        rensu.process(id, data2, Arrays.asList(new GoogleVisionProcessor(client),
                new StringTrimProcessor()), (id, result, exception) -> {
            this.recId = id;
            this.result = (String) result;
            this.exception = exception;
        });

        if (exception != null)
            log.error(exception.getMessage(), exception);

        assertNull(exception);
        assertEquals(id, recId);
        assertEquals("男「………落ち着けよ、そういう能力なんだろ」", result);
    }

}
