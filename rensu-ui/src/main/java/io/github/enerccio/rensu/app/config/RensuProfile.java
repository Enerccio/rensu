package io.github.enerccio.rensu.app.config;

import io.github.enerccio.rensu.ocr.OcrProcessor;
import io.github.enerccio.rensu.ocr.processors.ImageBrightnessContrastProcessor;
import io.github.enerccio.rensu.ocr.processors.ImageDesaturationProcessor;

import java.util.ArrayList;
import java.util.List;

public class RensuProfile {

    private String name;
    private float brightness = 1f;
    private int contrast = 0;
    private float saturation = 1.0f;
    private boolean vertical;
    private int pollingFrequency = 500;

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

    public List<OcrProcessor> toProcessors() {
        List<OcrProcessor> l = new ArrayList<>();
        l.add(new ImageDesaturationProcessor(getSaturation()));
        l.add(new ImageBrightnessContrastProcessor(getBrightness(), getContrast()));
        return l;
    }
}
