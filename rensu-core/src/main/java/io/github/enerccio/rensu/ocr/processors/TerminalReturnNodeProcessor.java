package io.github.enerccio.rensu.ocr.processors;

import io.github.enerccio.rensu.ocr.OcrProcessor;
import io.github.enerccio.rensu.ocr.OcrTaskChain;

public class TerminalReturnNodeProcessor implements OcrProcessor {

    @Override
    public void process(long id, byte[] input, OcrTaskChain chain) throws Exception {
        chain.finish(id, input);
    }

}
