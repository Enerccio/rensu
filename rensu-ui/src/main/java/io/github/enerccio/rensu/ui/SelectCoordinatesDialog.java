package io.github.enerccio.rensu.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class SelectCoordinatesDialog extends JPanel {

    private JFrame parentFrame;
    private GraphicsDevice device;
    private BufferedImage image;
    private int dragButton;
    private Dimension startMouseDrag;
    private Dimension endMouseDrag;
    private Consumer<Rectangle> callback;
    private Color grayColor;

    public void open(GraphicsDevice device, Consumer<Rectangle> callback) throws Exception {
        parentFrame = new JFrame();
        parentFrame.setContentPane(this);

        this.device = device;
        Robot robot = new Robot(device);
        this.callback = callback;
        grayColor = new Color(Color.GRAY.getRed(), Color.GRAY.getGreen(), Color.GRAY.getBlue(), 80);

        Dimension deviceDims = new Dimension(device.getDisplayMode().getWidth(), device.getDisplayMode().getHeight());
        image = robot.createScreenCapture(new Rectangle(deviceDims));

        setSize(deviceDims);
        parentFrame.setSize(deviceDims);
        parentFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//        parentFrame.setAlwaysOnTop(true);
        parentFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        parentFrame.setUndecorated(true);
        device.setFullScreenWindow(parentFrame);
        parentFrame.setVisible(true);

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                dragEvent(e);
                repaint();
            }
        });
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                releaseEvent(e);
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);

                dragButton = e.getButton();
            }
        });

        parentFrame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    parentFrame.dispose();
                    SwingUtilities.invokeLater(() -> callback.accept(null));
                }
            }
        });
    }

    private void releaseEvent(MouseEvent e) {
        if (e.getButton() == dragButton) {
            Rectangle rect = computeRectangle();
            SwingUtilities.invokeLater(() -> callback.accept(rect));
            parentFrame.dispose();
        } else {
            startMouseDrag = null;
            dragButton = 0;
            endMouseDrag = null;
            parentFrame.repaint();
        }
    }

    private void dragEvent(MouseEvent mouseEvent) {
        if (startMouseDrag == null) {
            startMouseDrag = new Dimension(mouseEvent.getX(), mouseEvent.getY());
        }
        endMouseDrag = new Dimension(mouseEvent.getX(), mouseEvent.getY());
        parentFrame.repaint();
    }


    @Override
    public void paint(Graphics g) {
        super.paint(g);

        g.drawImage(image, 0, 0, null);
        if (endMouseDrag != null) {
            g.setColor(Color.WHITE);
            Rectangle rectangle = computeRectangle();
            g.drawRect((int) rectangle.getX(), (int) rectangle.getY(), (int) rectangle.getWidth(), (int) rectangle.getHeight());
            g.setColor(grayColor);
            if (rectangle.getHeight() > 2 && rectangle.getWidth() > 2)
                g.fillRect((int) rectangle.getX() + 1, (int) rectangle.getY() + 1, (int) rectangle.getWidth() - 1, (int) rectangle.getHeight() - 1);
        }
    }

    private Rectangle computeRectangle() {
        int ax = (int) startMouseDrag.getWidth();
        int bx = (int) endMouseDrag.getWidth();
        int ay = (int) startMouseDrag.getHeight();
        int by = (int) endMouseDrag.getHeight();

        if (ax < bx) {
            if (ay < by)
                return new Rectangle(ax, ay, bx - ax, by - ay);
            else
                return new Rectangle(ax, by, bx - ax, ay - by);
        } else {
            if (ay < by)
                return new Rectangle(bx, ay, ax - bx, by - ay);
            else
                return new Rectangle(bx, by, ax - bx, ay - by);
        }
    }
}
