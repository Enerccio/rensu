package io.github.enerccio.rensu.test.ocr;

import io.github.enerccio.rensu.ocr.RensuOcr;
import io.github.enerccio.rensu.ocr.processors.ImageBrightnessContrastProcessor;
import io.github.enerccio.rensu.ocr.processors.ImageDesaturationProcessor;
import io.github.enerccio.rensu.ocr.processors.StringTrimProcessor;
import io.github.enerccio.rensu.ocr.processors.TikkaOcrProcessor;
import org.apache.commons.io.IOUtils;
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
public class TestImageManipCore {
    private static final Logger log = LoggerFactory.getLogger(TestImageManipCore.class);

    @Autowired
    private RensuOcr rensu;

    private byte[] data;
    private byte[] data2;
    private Throwable exception;
    private String result;
    private long id;
    private long recId;

    @BeforeEach
    public void setup() throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            InputStream is = TestImageManipCore.class.getResourceAsStream("/test2.png");
            IOUtils.copyLarge(is, bos);
            data = bos.toByteArray();
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            InputStream is = TestImageManipCore.class.getResourceAsStream("/test3.png");
            IOUtils.copyLarge(is, bos);
            data2 = bos.toByteArray();
        }
    }

    @Test
    public void noBrightnessTest() throws Exception {
        id = new Random().nextInt();
        rensu.process(id, data, Arrays.asList(new TikkaOcrProcessor(), new StringTrimProcessor()), (id, result, exception) -> {
            this.recId = id;
            this.result = result;
            this.exception = exception;
        });

        if (exception != null)
            log.error(exception.getMessage(), exception);
        assertNull(exception);
        assertEquals(id, recId);
        assertEquals("", result);
    }

    @Test
    public void brightnessAndContrastTest() throws Exception {
        ImageBrightnessContrastProcessor brightnessProcessor = new ImageBrightnessContrastProcessor(1.2f, 50);

        id = new Random().nextInt();
        rensu.process(id, data, Arrays.asList(brightnessProcessor, new TikkaOcrProcessor(), new StringTrimProcessor()), (id, result, exception) -> {
            this.recId = id;
            this.result = result;
            this.exception = exception;
        });

        if (exception != null)
            log.error(exception.getMessage(), exception);
        assertNull(exception);
        assertEquals(id, recId);
        assertEquals("男`-.....落ち着けよ、そういう能力なんだろ」", result);
    }

    @Test
    public void noDesatTest() throws Exception {
        id = new Random().nextInt();
        rensu.process(id, data2, Arrays.asList(new TikkaOcrProcessor(), new StringTrimProcessor()), (id, result, exception) -> {
            this.recId = id;
            this.result = result;
            this.exception = exception;
        });

        if (exception != null)
            log.error(exception.getMessage(), exception);
        assertNull(exception);
        assertEquals(id, recId);
        assertEquals("男`......落ち着けよ、そういう能力なんだろ」", result);
    }

    @Test
    public void desatTest() throws Exception {
        ImageDesaturationProcessor desaturationProcessor = new ImageDesaturationProcessor(0);

        id = new Random().nextInt();
        rensu.process(id, data2, Arrays.asList(desaturationProcessor, new TikkaOcrProcessor(), new StringTrimProcessor()), (id, result, exception) -> {
            this.recId = id;
            this.result = result;
            this.exception = exception;
        });

        if (exception != null)
            log.error(exception.getMessage(), exception);
        assertNull(exception);
        assertEquals(id, recId);
        assertEquals("男`......落ち着けよ、そういう能力なんだろ」", result);
    }

}
