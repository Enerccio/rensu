package io.github.enerccio.rensu.app.config;

import io.github.enerccio.rensu.app.HasApplicationContext;
import io.github.enerccio.rensu.ocr.OcrProcessor;
import io.github.enerccio.rensu.ocr.processors.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RensuProfile implements HasApplicationContext {

    private String name;
    private float brightness = 1f;
    private int contrast = 0;
    private float saturation = 1.0f;
    private boolean vertical;
    private int pollingFrequency = 500;
    private String ocr;
    private String tesseractLocation;
    private String tesseractDataLocation;
    private String tesseractLanguage;
    private byte[] googleCredentials;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getBrightness() {
        return brightness;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public int getContrast() {
        return contrast;
    }

    public void setContrast(int contrast) {
        this.contrast = contrast;
    }

    public float getSaturation() {
        return saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    public int getPollingFrequency() {
        return pollingFrequency;
    }

    public void setPollingFrequency(int pollingFrequency) {
        this.pollingFrequency = pollingFrequency;
    }

    public String getOcr() {
        return ocr;
    }

    public void setOcr(String ocr) {
        this.ocr = ocr;
    }

    public String getTesseractLocation() {
        return tesseractLocation;
    }

    public void setTesseractLocation(String tesseractLocation) {
        this.tesseractLocation = tesseractLocation;
    }

    public String getTesseractDataLocation() {
        return tesseractDataLocation;
    }

    public void setTesseractDataLocation(String tesseractDataLocation) {
        this.tesseractDataLocation = tesseractDataLocation;
    }

    public String getTesseractLanguage() {
        return tesseractLanguage;
    }

    public void setTesseractLanguage(String tesseractLanguage) {
        this.tesseractLanguage = tesseractLanguage;
    }

    public byte[] getGoogleCredentials() {
        return googleCredentials;
    }

    public void setGoogleCredentials(byte[] googleCredentials) {
        this.googleCredentials = googleCredentials;
    }

    public List<OcrProcessor> getOcrProcessorByProvider() {
        if (ocr == null || ocr.equals(Processors.TESSERACT)) {
            TesseractOcrProcessor processor = new TesseractOcrProcessor();
            if (tesseractLanguage != null) {
                processor.getConfig().setLanguage(tesseractLanguage);
            }
            if (tesseractLocation != null) {
                processor.getConfig().setTesseractPath(tesseractLocation);
            }
            if (tesseractDataLocation != null) {
                processor.getConfig().setTessdataPath(tesseractDataLocation);
            }
            return Arrays.asList(processor, new StringTrimAndReplaceProcessor());
        } else if (ocr.equals(Processors.GOOGLE_VISION)) {
            GoogleVisionProcessor processor = new GoogleVisionProcessor(getApplicationContext().getBean(GoogleVisionContainer.class).getClient(
                    new String(getGoogleCredentials(), StandardCharsets.UTF_8)));
            return Collections.singletonList(processor);
        }

        throw new IllegalStateException("Unknown processor " + ocr);
    }

    public List<OcrProcessor> toProcessors() {
        List<OcrProcessor> l = new ArrayList<>();
        l.add(new ImageDesaturationProcessor(getSaturation()));
        l.add(new ImageBrightnessContrastProcessor(getBrightness(), getContrast()));
        return l;
    }
}
