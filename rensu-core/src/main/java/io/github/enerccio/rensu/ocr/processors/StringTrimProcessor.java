package io.github.enerccio.rensu.ocr.processors;

import io.github.enerccio.rensu.ocr.OcrProcessor;
import io.github.enerccio.rensu.ocr.OcrTaskChain;

import java.nio.charset.StandardCharsets;

public class StringTrimProcessor implements OcrProcessor {

    @Override
    public void process(long id, byte[] input, OcrTaskChain chain) throws Exception {
        String i = new String(input, StandardCharsets.UTF_8);
        chain.next(id, i.replaceAll("\\s", "").getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return "StringTrimProcessor{}";
    }
}
