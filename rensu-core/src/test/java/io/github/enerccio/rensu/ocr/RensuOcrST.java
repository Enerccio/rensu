package io.github.enerccio.rensu.ocr;

public class RensuOcrST extends RensuOcr {

    @Override
    void enqueue(OcrProcessor processor, long id, byte[] data, OcrTaskChain ocrTaskChain) {
        try {
            processor.process(id, data, ocrTaskChain);
        } catch (Exception e) {
            ocrTaskChain.failure(id, e);
        }
    }

}
