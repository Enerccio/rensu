package io.github.enerccio.rensu.ocr.processors;

import io.github.enerccio.rensu.ocr.OcrProcessor;
import io.github.enerccio.rensu.ocr.OcrTaskChain;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ImageDesaturationProcessor implements OcrProcessor {

    private final float factor;

    public ImageDesaturationProcessor(float factor) {
        this.factor = factor;
    }

    public static int adjustSaturation(int argb, float factor) {
        float[] hsb = new float[3];
        int red = (argb >> 16) & 0xff;
        int green = (argb >> 8) & 0xff;
        int blue = argb & 0xff;
        Color.RGBtoHSB(red, green, blue, hsb);
        hsb[1] *= factor;
        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
    }

    @Override
    public void process(long id, byte[] input, OcrTaskChain chain) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(input));
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int argb = image.getRGB(x, y);
                image.setRGB(x, y, adjustSaturation(argb, factor));
            }
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bos);
            chain.next(id, bos.toByteArray());
        }
    }

    @Override
    public String toString() {
        return "ImageDesaturationProcessor{" +
                "factor=" + factor +
                '}';
    }
}
