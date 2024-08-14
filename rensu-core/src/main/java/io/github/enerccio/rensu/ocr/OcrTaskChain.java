package io.github.enerccio.rensu.ocr;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class OcrTaskChain {

    private final RensuOcr ocr;
    private final int pos;
    private final List<OcrProcessor> processors;
    private final OcrResultCallback callback;

    public OcrTaskChain(RensuOcr ocr, int pos, List<OcrProcessor> processors, OcrResultCallback callback) {
        this.ocr = ocr;
        this.processors = processors;
        this.callback = callback;
        this.pos = pos;
    }

    void run(long id, byte[] data) {
        try {
            if (pos < processors.size()) {
                OcrProcessor processor = processors.get(pos);
                ocr.enqueue(processor, id, data, this);
            } else {
                callback.onResult(id, new String(data, StandardCharsets.UTF_8), null);
            }
        } catch (Exception e) {
            callback.onResult(id, null, e);
        }
    }

    public void next(long id, byte[] data) {
        new OcrTaskChain(ocr, pos + 1, processors, callback).run(id, data);
    }

    void failure(long id, Exception e) {
        callback.onResult(id, null, e);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": processor=" + processors.get(pos);
    }
}
