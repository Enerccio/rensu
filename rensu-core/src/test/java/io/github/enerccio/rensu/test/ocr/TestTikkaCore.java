package io.github.enerccio.rensu.test.ocr;

import io.github.enerccio.rensu.ocr.RensuOcr;
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
public class TestTikkaCore {
    private static final Logger log = LoggerFactory.getLogger(TestTikkaCore.class);

    @Autowired
    private RensuOcr rensu;

    private byte[] data;
    private Throwable exception;
    private String result;
    private long id;
    private long recId;

    @BeforeEach
    public void setup() throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            InputStream is = TestTikkaCore.class.getResourceAsStream("/test1.png");
            IOUtils.copyLarge(is, bos);
            data = bos.toByteArray();
        }
    }

    @Test
    public void basicOcr() throws Exception {
        id = new Random().nextInt();
        rensu.process(id, data, Arrays.asList(new TikkaOcrProcessor(), new StringTrimProcessor()), (id, result, exception) -> {
            this.recId = id;
            this.result = (String) result;
            this.exception = exception;
        });

        if (exception != null)
            log.error(exception.getMessage(), exception);

        assertNull(exception);
        assertEquals(id, recId);
        assertEquals("過去に着用した衣装、描いた絵画、写真など彼女の思い出を辿るイベントとなります。", result);
    }

}
