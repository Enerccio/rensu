package io.github.enerccio.rensu.ocr.processors;

import io.github.enerccio.rensu.ocr.OcrProcessor;
import io.github.enerccio.rensu.ocr.OcrTaskChain;
import io.github.enerccio.rensu.ocr.OutputType;
import io.github.enerccio.rensu.ocr.StreamGobbler;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CustomOCRProcessor implements OcrProcessor {

    private final String process;

    public CustomOCRProcessor(String process) {
        this.process = process;
    }

    @Override
    public void process(long id, byte[] input, OcrTaskChain chain) throws Exception {
        File tmpCopy = File.createTempFile("img", "");
        tmpCopy.deleteOnExit();
        FileUtils.writeByteArrayToFile(tmpCopy, input);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(process, tmpCopy.getAbsolutePath());
            processBuilder.redirectError(Redirect.PIPE);
            processBuilder.redirectOutput(Redirect.PIPE);

            Process proc = processBuilder.start();
            List<String> output = new ArrayList<>();
            StreamGobbler.gobble(proc, (line, type) -> {
                if (type == OutputType.STDERR)
                    System.err.println(line);
                else
                    output.add(line);
            });
            int i = proc.waitFor();
            if (i != 0)
                throw new RuntimeException("Process failed");
            String text = String.join("\n", output);
            chain.next(id, text.getBytes(StandardCharsets.UTF_8));
        } finally {
            tmpCopy.delete();
        }
    }

}
