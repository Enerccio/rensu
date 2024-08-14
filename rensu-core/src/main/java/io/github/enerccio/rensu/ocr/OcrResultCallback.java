package io.github.enerccio.rensu.ocr;

public interface OcrResultCallback {

    void onResult(long id, Object result, Throwable exception);

}
