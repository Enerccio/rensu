package io.github.enerccio.rensu.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash;
import io.github.enerccio.rensu.app.config.ConfigContainer;
import io.github.enerccio.rensu.app.config.RensuProfile;
import io.github.enerccio.rensu.ocr.OcrProcessor;
import io.github.enerccio.rensu.ocr.RensuOcr;
import io.github.enerccio.rensu.ocr.processors.StringTrimProcessor;
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Poller extends Thread implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(Poller.class);
    private static final Gson gson = new GsonBuilder().create();
    private final HashingAlgorithm hasher = new PerceptiveHash(32);
    boolean hasWs = false;
    @Autowired
    private ConfigContainer configContainer;
    @Autowired
    private RensuOcr ocr;
    private WebSocketServer server;
    private GraphicsDevice screen;
    private Rectangle screenRegion;
    private Robot robot;
    private BufferedImage lastImage;
    private Hash lastImageHash;
    private int pollingSpeed = 1000;
    private volatile boolean process;
    private List<OcrProcessor> processors;
    private List<WebSocket> clients = new ArrayList<>();

    private volatile String lastText;
    private volatile long lastId = Long.MIN_VALUE;
    private boolean enableHashing = false;

    @SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
    @Override
    public void run() {
        try {
            long cId = Long.MIN_VALUE + 1;

            while (true) {
                Thread.sleep(10);
                if (process) {
                    Thread.sleep(pollingSpeed - 10);

                    long fcid = cId;
                    ++cId;

                    SwingUtilities.invokeLater(() -> {
                        try {
                            if (takeScreenCapture()) {
                                BufferedImage cImage = lastImage;
                                byte[] data;
                                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                                    ImageIO.write(cImage, "png", out);
                                    data = out.toByteArray();
                                }

                                ocr.process(fcid, data, processors, (id, result, exception) -> {
                                    if (exception == null) {
                                        finishOcr(id, (String) result);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            log.error(e.getMessage());
                            log.debug(e.getMessage(), e);
                        }
                    });
                } else {
                    cId = Long.MIN_VALUE + 1;
                    lastId = Long.MIN_VALUE;
                    lastText = null;
                }
            }
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private synchronized void finishOcr(long id, String result) {
        if (lastId > id)
            return; // ignore old result

        if (!result.equals(lastText) && StringUtils.isNotBlank(result)) {
            lastId = id;
            lastText = result;
            StringSelection selection = new StringSelection(lastText);

            try {
                if (hasWs) {
                    server.broadcast(result);
                }
            } catch (Exception e) {
                // ignore WS error
            }

            SwingUtilities.invokeLater(() -> {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);

                try {
                    clipboard.getContents(null).getTransferData(DataFlavor.stringFlavor);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public void setRegion(GraphicsDevice device, Rectangle screenRegion) throws Exception {
        this.screen = device;
        this.screenRegion = screenRegion;
        this.robot = new Robot(screen);
        lastImage = null;
        lastImageHash = null;

        takeScreenCapture();
    }

    private boolean takeScreenCapture() {
        BufferedImage bufferedImage = robot.createScreenCapture(screenRegion);
        if (lastImage == null || !enableHashing) {
            lastImage = bufferedImage;
            lastImageHash = hasher.hash(lastImage);
            return true;
        } else {
            Hash hash = hasher.hash(bufferedImage);
            if (lastImageHash.normalizedHammingDistanceFast(hash) < 0.1) {
                return false;
            } else {
                lastImage = bufferedImage;
                lastImageHash = hash;
                return true;
            }
        }
    }

    public synchronized BufferedImage takeCapture() {
        takeScreenCapture();
        return lastImage;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            server = new WebSocketServer(new InetSocketAddress(6677)) {
                @Override
                public void onOpen(WebSocket conn, ClientHandshake handshake) {
                    log.info("Something connected {} {}", conn, handshake);
                    clients.add(conn);
                }

                @Override
                public void onClose(WebSocket conn, int code, String reason, boolean remote) {

                }

                @Override
                public void onMessage(WebSocket conn, String message) {
                    log.info("message: " + message);
                }

                @Override
                public void onError(WebSocket conn, Exception ex) {
                    log.error(ex.getMessage(), ex);
                }

                @Override
                public void onStart() {

                }
            };
            server.setDaemon(true);
            server.start();
            hasWs = true;
        } catch (Exception e) {
            hasWs = false;
        }

        setDaemon(true);
        setName("Poller thread");
        start();
    }

    public Rectangle getRegion() {
        return screenRegion;
    }

    public GraphicsDevice getDevice() {
        return screen;
    }

    public void clear() {
        process = false;

        screenRegion = null;
        screen = null;
        lastImage = null;
        lastImageHash = null;
        robot = null;
    }

    public void setProcess(boolean process) {
        if (process) {
            // TODO: profiles
            RensuProfile profile = configContainer.getGlobalProfile();

            List<OcrProcessor> pl = profile.toProcessors();
            processors = new ArrayList<>(pl);
            processors.add(profile.getOcrProcessorByProvider());
            processors.add(new StringTrimProcessor());

            lastImage = null;
            lastImageHash = null;
        }
        this.process = process;
    }

    @Override
    public void destroy() throws Exception {
        interrupt();
    }

}
