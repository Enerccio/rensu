package io.github.enerccio.rensu.ocr.processors;

import io.github.enerccio.rensu.ocr.OcrProcessor;
import io.github.enerccio.rensu.ocr.OcrTaskChain;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class TikkaOcrProcessor implements OcrProcessor {

    private Parser imageParser;
    private TesseractOCRConfig config;
    private PDFParserConfig pdfConfig;
    private Metadata metadata;

    public TikkaOcrProcessor() {
        config = new TesseractOCRConfig();
        config.setLanguage("jpn");
        imageParser = new AutoDetectParser(TikaConfig.getDefaultConfig());
        pdfConfig = new PDFParserConfig();
        pdfConfig.setExtractInlineImages(true);
        metadata = new Metadata();
    }

    @Override
    public void process(long id, byte[] input, OcrTaskChain chain) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BodyContentHandler handler = new BodyContentHandler(out);
            ParseContext context = new ParseContext();
            context.set(Parser.class, imageParser);
            context.set(PDFParserConfig.class, pdfConfig);
            context.set(TesseractOCRConfig.class, config);
            imageParser.parse(new ByteArrayInputStream(input), handler, metadata, context);
            chain.next(id, out.toByteArray());
        }
    }

    public Parser getImageParser() {
        return imageParser;
    }

    public void setImageParser(Parser imageParser) {
        this.imageParser = imageParser;
    }

    public TesseractOCRConfig getConfig() {
        return config;
    }

    public void setConfig(TesseractOCRConfig config) {
        this.config = config;
    }

    public PDFParserConfig getPdfConfig() {
        return pdfConfig;
    }

    public void setPdfConfig(PDFParserConfig pdfConfig) {
        this.pdfConfig = pdfConfig;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "TikkaOcrProcessor{" +
                "imageParser=" + imageParser +
                ", config=" + config +
                ", pdfConfig=" + pdfConfig +
                ", metadata=" + metadata +
                '}';
    }
}
