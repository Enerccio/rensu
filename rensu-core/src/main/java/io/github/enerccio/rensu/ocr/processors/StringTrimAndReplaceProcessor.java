package io.github.enerccio.rensu.ocr.processors;

import io.github.enerccio.rensu.ocr.OcrProcessor;
import io.github.enerccio.rensu.ocr.OcrTaskChain;

import java.nio.charset.StandardCharsets;

public class StringTrimAndReplaceProcessor implements OcrProcessor {

    @Override
    public void process(long id, byte[] input, OcrTaskChain chain) throws Exception {
        String i = new String(input, StandardCharsets.UTF_8);
        chain.finish(id, i.replaceAll("\\s", "").replaceAll("\\p{Punct}", ""));
    }

    @Override
    public String toString() {
        return "StringTrimProcessor{}";
    }
}
