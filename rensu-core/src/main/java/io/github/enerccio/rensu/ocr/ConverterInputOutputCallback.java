package io.github.enerccio.rensu.ocr;

@FunctionalInterface
public interface ConverterInputOutputCallback {

    void onRead(String line, OutputType type);

}