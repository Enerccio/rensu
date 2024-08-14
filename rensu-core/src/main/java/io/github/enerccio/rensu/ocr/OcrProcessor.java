package io.github.enerccio.rensu.ocr;

public interface OcrProcessor {

    void process(long id, byte[] input, OcrTaskChain chain) throws Exception;

}
