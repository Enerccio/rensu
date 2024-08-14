package io.github.enerccio.rensu.ocr.processors;

import io.github.enerccio.rensu.ocr.OcrProcessor;
import io.github.enerccio.rensu.ocr.OcrTaskChain;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ImageBrightnessContrastProcessor implements OcrProcessor {

    private final float brightness;
    private final int contrast;

    public ImageBrightnessContrastProcessor(float brightness, int contrast) {
        this.brightness = brightness;
        this.contrast = contrast;
    }

    @Override
    public void process(long id, byte[] input, OcrTaskChain chain) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(input));
        RescaleOp rescaleOp = new RescaleOp(brightness, contrast, null);
        rescaleOp.filter(image, image);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bos);
            chain.next(id, bos.toByteArray());
        }
    }

    @Override
    public String toString() {
        return "ImageBrightnessContrastProcessor{" +
                "brightness=" + brightness +
                ", contrast=" + contrast +
                '}';
    }
}
