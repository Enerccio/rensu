package io.github.enerccio.rensu.test.ocr;

import io.github.enerccio.rensu.ocr.RensuOcr;
import io.github.enerccio.rensu.ocr.processors.CustomOCRProcessor;
import io.github.enerccio.rensu.ocr.processors.StringTrimProcessor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SuppressWarnings("DataFlowIssue")
@SpringJUnitConfig(locations = "/test-config.xml")
public class TestCustomCore {
    private static final Logger log = LoggerFactory.getLogger(TestCustomCore.class);

    @Autowired
    private RensuOcr rensu;

    private byte[] data;
    private Throwable exception;
    private String result;
    private long id;
    private long recId;
    private File processFile;

    @BeforeEach
    public void setup() throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            InputStream is = TestCustomCore.class.getResourceAsStream("/test1.png");
            IOUtils.copyLarge(is, bos);
            data = bos.toByteArray();
        }

        processFile = File.createTempFile("test", ".sh");
        processFile.deleteOnExit();
        processFile.setExecutable(true);

        FileUtils.writeByteArrayToFile(processFile, IOUtils.toByteArray(TestCustomCore.class.getResourceAsStream("/test.sh")));
    }

    @Test
    public void basicOcr() throws Exception {
        id = new Random().nextInt();
        rensu.process(id, data, Arrays.asList(new CustomOCRProcessor(processFile.getAbsolutePath()), new StringTrimProcessor()), (id, result, exception) -> {
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
