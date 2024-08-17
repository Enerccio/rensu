package io.github.enerccio.rensu.ui;

import io.github.enerccio.rensu.app.HasApplicationContext;
import io.github.enerccio.rensu.app.Poller;
import io.github.enerccio.rensu.app.config.ConfigContainer;
import io.github.enerccio.rensu.app.config.Processors;
import io.github.enerccio.rensu.app.config.RensuProfile;
import io.github.enerccio.rensu.ocr.OcrProcessor;
import io.github.enerccio.rensu.ocr.RensuOcrST;
import io.github.enerccio.rensu.ocr.processors.GoogleVisionProcessor;
import io.github.enerccio.rensu.ocr.processors.TerminalReturnNodeProcessor;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.github.enerccio.rensu.ocr.processors.TesseractOcrProcessor.LANGUAGE_JPN;
import static io.github.enerccio.rensu.ocr.processors.TesseractOcrProcessor.LANGUAGE_JPN_VERT;

public class MainWindow extends JFrame implements HasApplicationContext {

    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    private final RensuOcrST ocrST = new RensuOcrST();
    private JComboBox<GraphicsDeviceWithName> deviceSelector;
    private JLabel screenGrabX, screenGrabY, screenGrabW, screenGrabH, screenGrabDev;
    private JLabel previewImage;
    private JLabel afterChangesImage;
    private JButton grabNewArea;
    private JButton retakeImage;
    private JButton refresh;
    private JSlider contrast;
    private JSlider brightness;
    private JSlider desaturation;
    private JButton start;
    private JButton stop;
    private JComboBox<String> ocr;
    private JComboBox<String> tesseractLanguage;
    private JTextField tesseractPath;
    private JTextField tesseractDataPath;
    private JButton googleJsonFileLocation;
    private JTextField customCommand;
    private JTextArea output;

    private BufferedImage testImage;
    private boolean inLoad = false;
    private String lastCredentials;

    private void disableAllControls() {
        deviceSelector.setEnabled(false);
        grabNewArea.setEnabled(false);
        retakeImage.setEnabled(false);
        refresh.setEnabled(false);
        contrast.setEnabled(false);
        brightness.setEnabled(false);
        desaturation.setEnabled(false);

        ocr.setEnabled(false);
        if (ocr.getSelectedItem() == Processors.TESSERACT) {
            tesseractLanguage.setEnabled(false);
            tesseractPath.setEnabled(false);
            tesseractDataPath.setEnabled(false);
        } else if (ocr.getSelectedItem() == Processors.GOOGLE_VISION) {
            googleJsonFileLocation.setEnabled(false);
        } else if (ocr.getSelectedItem() == Processors.CUSTOM) {
            customCommand.setEnabled(false);
        }
    }

    private void enableAllControls() {
        deviceSelector.setEnabled(true);
        grabNewArea.setEnabled(true);
        retakeImage.setEnabled(true);
        refresh.setEnabled(true);
        contrast.setEnabled(true);
        brightness.setEnabled(true);
        desaturation.setEnabled(true);

        ocr.setEnabled(true);
        if (ocr.getSelectedItem() == Processors.TESSERACT) {
            tesseractLanguage.setEnabled(true);
            tesseractPath.setEnabled(true);
            tesseractDataPath.setEnabled(true);
        } else if (ocr.getSelectedItem() == Processors.GOOGLE_VISION) {
            googleJsonFileLocation.setEnabled(true);
        } else if (ocr.getSelectedItem() == Processors.CUSTOM) {
            customCommand.setEnabled(true);
        }
    }

    public void create() throws Exception {
        setTitle("レンス");
        setAlwaysOnTop(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        createDisplaySelector();
        createAreaSelector();
        createTestFrame();
        createOcrSection();
        loadFromSettings();

        getApplicationContext().getBean(Poller.class).setCallback(text -> {
            output.append("\n" + text);
            output.setCaretPosition(output.getDocument().getLength());
        });

        pack();
    }

    private void loadFromSettings() {
        loadFromSettings(false);
    }

    private void loadFromSettings(boolean skipOcrSelection) {
        inLoad = true;

        try {
            // TODO: multiple profiles
            ConfigContainer cc = getApplicationContext().getBean(ConfigContainer.class);
            RensuProfile profile = cc.getGlobalProfile();
            contrast.setValue(profile.getContrast());
            brightness.setValue((int) (127 * profile.getBrightness()));
            desaturation.setValue((int) (255 * profile.getSaturation()));

            if (!skipOcrSelection) {
                if (profile.getOcr() == null || profile.getOcr().equals(Processors.TESSERACT)) {
                    ocr.setSelectedItem(Processors.TESSERACT);
                } else if (Processors.GOOGLE_VISION.equals(profile.getOcr())) {
                    ocr.setSelectedItem(Processors.GOOGLE_VISION);
                } else if (Processors.CUSTOM.equals(profile.getOcr())) {
                    ocr.setSelectedItem(Processors.CUSTOM);
                }
            }

            if (ocr.getSelectedItem() == Processors.TESSERACT) {
                tesseractLanguage.setSelectedItem(profile.getTesseractLanguage() == null ?
                        LANGUAGE_JPN : profile.getTesseractLanguage());
                tesseractDataPath.setText(profile.getTesseractDataLocation());
                tesseractPath.setText(profile.getTesseractLocation());
            } else if (ocr.getSelectedItem() == Processors.CUSTOM) {
                customCommand.setText(profile.getCustomCommand());
            } else if (ocr.getSelectedItem() == Processors.GOOGLE_VISION) {
                if (profile.getGoogleCredentials() != null) {
                    googleJsonFileLocation.setText("Upload json credentials - SET");
                }
            }
        } finally {
            inLoad = false;
        }
    }

    private void createOcrSection() {
        JPanel mainSection = new JPanel();
        getContentPane().add(mainSection);
        mainSection.setLayout(new BoxLayout(mainSection, BoxLayout.Y_AXIS));
        mainSection.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "OCR"));

        JPanel settingsPanel = new JPanel();
        settingsPanel.setBorder(BorderFactory.createCompoundBorder());
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));

        ocr = new JComboBox<>();
        ocr.addItem(Processors.TESSERACT);
        ocr.addItem(Processors.GOOGLE_VISION);
        ocr.addItem(Processors.CUSTOM);
        ocr.addActionListener(event -> {
            settingsPanel.removeAll();
            if (ocr.getSelectedItem() == Processors.TESSERACT) {
                loadTesseractSettings(settingsPanel);
            } else if (ocr.getSelectedItem() == Processors.GOOGLE_VISION) {
                loadGoogleVisionSettings(settingsPanel);
            } else if (ocr.getSelectedItem() == Processors.CUSTOM) {
                loadCustomSettings(settingsPanel);
            }
            loadFromSettings(true);
            saveSettings();
            pack();
        });

        mainSection.add(ocr);
        mainSection.add(settingsPanel);

        JPanel buttons = new JPanel();
        mainSection.add(buttons);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

        start = new JButton("Start");
        buttons.add(start);
        start.addActionListener(event -> {
            start.setEnabled(false);
            getApplicationContext().getBean(Poller.class).setProcess(true);
            disableAllControls();
            stop.setEnabled(true);
        });
        start.setEnabled(false);

        stop = new JButton("Stop");
        buttons.add(stop);
        stop.addActionListener(event -> {
            stop.setEnabled(false);
            getApplicationContext().getBean(Poller.class).setProcess(false);
            enableAllControls();
            start.setEnabled(true);
        });
        stop.setEnabled(false);

        output = new JTextArea();
        JScrollPane pane = new JScrollPane(output);
        pane.createVerticalScrollBar();
        pane.setMinimumSize(new Dimension(600, 450));
        pane.setPreferredSize(new Dimension(600, 450));
        output.setEnabled(true);
        DefaultCaret caret = (DefaultCaret) output.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        mainSection.add(pane);
    }

    private void loadTesseractSettings(JPanel settingsPanel) {
        tesseractLanguage = new JComboBox<>();
        tesseractLanguage.addItem(LANGUAGE_JPN);
        tesseractLanguage.addItem(LANGUAGE_JPN_VERT);
        tesseractLanguage.addActionListener(event -> saveSettings());
        settingsPanel.add(new JLabel("Language: "));
        settingsPanel.add(tesseractLanguage);

        tesseractPath = new JTextField();
        tesseractPath.addActionListener(event -> saveSettings());
        settingsPanel.add(new JLabel("Tesseract path: "));
        settingsPanel.add(tesseractPath);
        tesseractDataPath = new JTextField();
        tesseractDataPath.addActionListener(event -> saveSettings());
        settingsPanel.add(new JLabel("Tesseract data path: "));
        settingsPanel.add(tesseractDataPath);
    }

    private void loadGoogleVisionSettings(JPanel settingsPanel) {
        googleJsonFileLocation = new JButton("Upload json credentials");
        googleJsonFileLocation.addActionListener(event -> loadGoogleCredentials());
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(googleJsonFileLocation, BorderLayout.CENTER);
        settingsPanel.add(panel);
    }

    private void loadCustomSettings(JPanel settingsPanel) {
        settingsPanel.add(new JLabel("Custom command: "));
        customCommand = new JTextField("Custom command");
        customCommand.addActionListener(event -> saveSettings());
        settingsPanel.add(customCommand);
    }

    private void loadGoogleCredentials() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isFile() && f.getName().endsWith(".json");
            }

            @Override
            public String getDescription() {
                return "json";
            }
        });
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            String jsonData;
            try {
                jsonData = FileUtils.readFileToString(fileChooser.getSelectedFile(), StandardCharsets.UTF_8);
                GoogleVisionProcessor.testCredentials(jsonData);
            } catch (Throwable e) {
                UIUtils.onError(this, log, e, "Invalid credentials!");
                return;
            }
            lastCredentials = jsonData;
            googleJsonFileLocation.setText("Upload json credentials - SET");
            saveSettings();
        }
    }

    private void createTestFrame() {
        JPanel mainSection = new JPanel();
        getContentPane().add(mainSection);
        mainSection.setLayout(new BoxLayout(mainSection, BoxLayout.Y_AXIS));
        mainSection.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Screen grabbing options"));

        JPanel previewImagePanel = new JPanel();
        mainSection.add(previewImagePanel);
        previewImagePanel.setLayout(new BorderLayout());
        previewImagePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED), "Original image"));

        previewImage = new JLabel();
        previewImagePanel.add(previewImage, BorderLayout.CENTER);

        retakeImage = new JButton("Refresh");
        retakeImage.addActionListener(event -> retakePreviewImage());
        previewImagePanel.add(retakeImage, BorderLayout.EAST);
        retakeImage.setEnabled(false);

        mainSection.add(new JLabel("<html><b>Saturation<b></html>"));
        desaturation = new JSlider();
        desaturation.setMinimum(0);
        desaturation.setMaximum(255);
        mainSection.add(desaturation);

        mainSection.add(new JLabel("<html><b>Contrast<b></html>"));
        contrast = new JSlider();
        contrast.setMinimum(0);
        contrast.setMaximum(255);
        mainSection.add(contrast);

        mainSection.add(new JLabel("<html><b>Brightness<b></html>"));
        brightness = new JSlider();
        brightness.setMinimum(0);
        brightness.setMaximum(255);
        mainSection.add(brightness);

        desaturation.addChangeListener(event -> {
            saveSettings();
            applyEffects();
        });
        contrast.addChangeListener(event -> {
            saveSettings();
            applyEffects();
        });
        brightness.addChangeListener(event -> {
            saveSettings();
            applyEffects();
        });

        JPanel afterImagePanel = new JPanel();
        mainSection.add(afterImagePanel);
        afterImagePanel.setLayout(new BorderLayout());
        afterImagePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED), "After image"));

        afterChangesImage = new JLabel();
        afterImagePanel.add(afterChangesImage, BorderLayout.CENTER);
    }

    private void saveSettings() {
        if (inLoad)
            return;

        // TODO: profiles

        ConfigContainer cc = getApplicationContext().getBean(ConfigContainer.class);
        RensuProfile profile = cc.getGlobalProfile();
        profile.setContrast(contrast.getValue());
        profile.setBrightness((brightness.getValue() / 127f));
        profile.setSaturation(desaturation.getValue() / 255.0f);

        if (ocr.getSelectedItem() == null) {
            profile.setOcr(Processors.TESSERACT);
            profile.setTesseractDataLocation(null);
            profile.setTesseractLanguage("jpn");
            profile.setTesseractLocation(null);
        } else if (ocr.getSelectedItem() == Processors.TESSERACT) {
            profile.setOcr(Processors.TESSERACT);
            profile.setTesseractDataLocation(tesseractDataPath.getText());
            profile.setTesseractLanguage(tesseractLanguage.getSelectedItem() == null ? "jpn" : (String) tesseractLanguage.getSelectedItem());
            profile.setTesseractLocation(tesseractPath.getText());
        } else if (ocr.getSelectedItem() == Processors.GOOGLE_VISION) {
            profile.setOcr(Processors.GOOGLE_VISION);
            if (lastCredentials != null)
                profile.setGoogleCredentials(lastCredentials.getBytes(StandardCharsets.UTF_8));
        } else if (ocr.getSelectedItem() == Processors.CUSTOM) {
            profile.setOcr(Processors.CUSTOM);
            profile.setCustomCommand(customCommand.getText());
        }

        try {
            cc.save();
        } catch (Exception e) {
            UIUtils.onError(this, log, e, "Failed to save settings");
        }
    }

    private void retakePreviewImage() {
        Poller poller = getApplicationContext().getBean(Poller.class);
        if (poller.getDevice() != null) {
            try {
                testImage = poller.takeCapture();
            } catch (AWTException e) {
                UIUtils.onError(this, log, e, e.getMessage());
            }
        }
        applyEffects();
        pack();
    }

    private void resetPreviews() {
        if (testImage == null) {
            previewImage.setIcon(null);
        } else {
            previewImage.setIcon(new ImageIcon(testImage));
        }
        applyEffects();
        pack();
    }

    private void applyEffects() {
        if (testImage == null) {
            afterChangesImage.setIcon(null);
        } else {
            try {
                List<OcrProcessor> baseProcessors = getBaseProcessors();
                baseProcessors.add(new TerminalReturnNodeProcessor());
                byte[] data;
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    ImageIO.write(testImage, "png", out);
                    data = out.toByteArray();
                }
                ocrST.process(0, data, baseProcessors, (id, result, exception) -> {
                    if (exception == null) {
                        try {
                            BufferedImage image = ImageIO.read(new ByteArrayInputStream((byte[]) result));
                            afterChangesImage.setIcon(new ImageIcon(image));
                        } catch (Exception e) {
                            UIUtils.onError(this, log, e, "Failed to read image");
                        }
                    }
                });
            } catch (Exception e) {
                UIUtils.onError(this, log, e, "Failed to process image");
            }
        }
    }

    private List<OcrProcessor> getBaseProcessors() {
        // TODO: profiles
        ConfigContainer cc = getApplicationContext().getBean(ConfigContainer.class);
        RensuProfile profile = cc.getGlobalProfile();

        return profile.toProcessors();
    }

    private void createAreaSelector() {
        JPanel mainSection = new JPanel();
        getContentPane().add(mainSection);
        BorderLayout bl = new BorderLayout();
        bl.setVgap(25);
        mainSection.setLayout(bl);
        mainSection.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Grab area"));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.X_AXIS));
        screenGrabDev = new JLabel();
        info.add(screenGrabDev);
        screenGrabX = new JLabel();
        info.add(screenGrabX);
        screenGrabY = new JLabel();
        info.add(screenGrabY);
        screenGrabW = new JLabel();
        info.add(screenGrabW);
        screenGrabH = new JLabel();
        info.add(screenGrabH);
        mainSection.add(info, BorderLayout.CENTER);

        reloadInfo();

        grabNewArea = new JButton("Select region");
        grabNewArea.addActionListener(event -> {
            SelectCoordinatesDialog dialog = new SelectCoordinatesDialog();
            try {
                GraphicsDeviceWithName deviceWithName = (GraphicsDeviceWithName) deviceSelector.getSelectedItem();
                assert deviceWithName != null;

                dialog.open(deviceWithName.device, rectangle -> {
                    Poller poller = getApplicationContext().getBean(Poller.class);
                    if (rectangle != null) {
                        try {
                            poller.setRegion(deviceWithName.device, rectangle);
                            retakePreviewImage();
                            reloadInfo();
                            resetPreviews();
                            retakeImage.setEnabled(true);
                            start.setEnabled(true);
                        } catch (Exception e) {
                            UIUtils.onError(this, log, e, "Failed to create screen grabber");
                        }
                    } else {
                        testImage = null;
                        poller.clear();
                        retakeImage.setEnabled(false);
                        start.setEnabled(false);
                        reloadInfo();
                        resetPreviews();
                    }
                });
            } catch (Exception e) {
                UIUtils.onError(this, log, e, "Failed to open select region window");
            }
        });
        mainSection.add(grabNewArea, BorderLayout.EAST);
    }

    private void reloadInfo() {
        Poller poller = getApplicationContext().getBean(Poller.class);
        Rectangle region = poller.getRegion();
        GraphicsDevice device = poller.getDevice();
        if (region == null) {
            screenGrabDev.setText("<html><b>Display: </b> Not set</html>");
            screenGrabX.setText("<html><b>X: </b> Not set</html>");
            screenGrabY.setText("<html><b>Y: </b> Not set</html>");
            screenGrabW.setText("<html><b>W: </b> Not set</html>");
            screenGrabH.setText("<html><b>H: </b> Not set</html>");
        } else {
            screenGrabDev.setText("<html><b>Display: </b> " + device.getIDstring() + " </html>");
            screenGrabX.setText("<html><b>X: </b> " + region.getX() + " </html>");
            screenGrabY.setText("<html><b>Y: </b> " + region.getY() + "</html>");
            screenGrabW.setText("<html><b>W: </b> " + region.getWidth() + "</html>");
            screenGrabH.setText("<html><b>H: </b> " + region.getHeight() + "</html>");
        }
    }

    private void createDisplaySelector() {
        JPanel mainSection = new JPanel();
        getContentPane().add(mainSection);
        mainSection.setLayout(new BorderLayout());
        mainSection.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Display selection"));

        deviceSelector = new JComboBox<>();
        deviceSelector.setPreferredSize(new Dimension(500, 30));
        loadDevices();
        mainSection.add(deviceSelector, BorderLayout.CENTER);

        refresh = new JButton("Refresh");
        mainSection.add(refresh, BorderLayout.EAST);
        refresh.addActionListener(e -> {
            loadDevices();
        });
    }

    private void loadDevices() {
        deviceSelector.removeAllItems();

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (GraphicsDevice device : ge.getScreenDevices()) {
            deviceSelector.addItem(new GraphicsDeviceWithName(device));
        }
    }

    private record GraphicsDeviceWithName(GraphicsDevice device) {

        @Override
        public String toString() {
            return device.getIDstring();
        }
    }
}
