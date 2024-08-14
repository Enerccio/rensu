package io.github.enerccio.rensu.ocr;

public interface OcrResultCallback {

    void onResult(long id, String result, Throwable exception);

}
