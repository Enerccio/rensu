package io.github.enerccio.rensu.ui;

import io.github.enerccio.rensu.app.HasApplicationContext;
import io.github.enerccio.rensu.app.Poller;
import io.github.enerccio.rensu.app.config.ConfigContainer;
import io.github.enerccio.rensu.app.config.RensuProfile;
import io.github.enerccio.rensu.ocr.OcrProcessor;
import io.github.enerccio.rensu.ocr.RensuOcrST;
import io.github.enerccio.rensu.ocr.processors.TerminalReturnNodeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

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

    private BufferedImage testImage;

    private void disableAllControls() {
        deviceSelector.setEnabled(false);
        grabNewArea.setEnabled(false);
        retakeImage.setEnabled(false);
        refresh.setEnabled(false);
        contrast.setEnabled(false);
        brightness.setEnabled(false);
        desaturation.setEnabled(false);
    }

    private void enableAllControls() {
        deviceSelector.setEnabled(true);
        grabNewArea.setEnabled(true);
        retakeImage.setEnabled(true);
        refresh.setEnabled(true);
        contrast.setEnabled(true);
        brightness.setEnabled(true);
        desaturation.setEnabled(true);
    }

    public void create() throws Exception {
        setTitle("レンス");
        setAlwaysOnTop(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        createDisplaySelector();
        createAreaSelector();
        createTestFrame();
        createButtons();

        pack();
    }

    private void createButtons() {
        JPanel mainSection = new JPanel();
        getContentPane().add(mainSection);
        mainSection.setLayout(new BoxLayout(mainSection, BoxLayout.X_AXIS));
        mainSection.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "OCR"));

        start = new JButton("Start");
        mainSection.add(start);
        start.addActionListener(event -> {
            start.setEnabled(false);
            getApplicationContext().getBean(Poller.class).setProcess(true);
            disableAllControls();
            stop.setEnabled(true);
        });
        start.setEnabled(false);

        stop = new JButton("Stop");
        mainSection.add(stop);
        stop.addActionListener(event -> {
            stop.setEnabled(false);
            getApplicationContext().getBean(Poller.class).setProcess(false);
            enableAllControls();
            start.setEnabled(true);
        });
        stop.setEnabled(false);
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

        // TODO: multiple profiles
        ConfigContainer cc = getApplicationContext().getBean(ConfigContainer.class);
        RensuProfile profile = cc.getGlobalProfile();
        contrast.setValue(profile.getContrast());
        brightness.setValue((int) (127 * profile.getBrightness()));
        desaturation.setValue((int) (255 * profile.getSaturation()));

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
        // TODO: profiles

        ConfigContainer cc = getApplicationContext().getBean(ConfigContainer.class);
        RensuProfile profile = cc.getGlobalProfile();
        profile.setContrast(contrast.getValue());
        profile.setBrightness((brightness.getValue() / 127f));
        profile.setSaturation(desaturation.getValue() / 255.0f);

        try {
            cc.save();
        } catch (Exception e) {
            UIUtils.onError(log, e, "Failed to save settings");
        }
    }

    private void retakePreviewImage() {
        Poller poller = getApplicationContext().getBean(Poller.class);
        if (poller.getDevice() != null) {
            testImage = poller.takeCapture();
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
                            UIUtils.onError(log, e, "Failed to read image");
                        }
                    }
                });
            } catch (Exception e) {
                UIUtils.onError(log, e, "Failed to process image");
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
                            UIUtils.onError(log, e, "Failed to create screen grabber");
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
                UIUtils.onError(log, e, "Failed to open select region window");
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
