package io.github.enerccio.rensu.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class ErrorDialog extends JDialog {

    private static final String SHOW_DETAILS_TEXT = "Show Details ...";
    private static final String HIDE_DETAILS_TEXT = "Hide Details";
    private final JButton jButtonClose;
    private final JButton jButtonShowHideDetails;
    private final JPanel jPanelBottom;
    private final JPanel jPanelCenter;
    private final JPanel jPanelTop;
    private final JScrollPane jScrollPaneErrorMsg;
    private final JTextPane jTextPaneErrorMsg;
    private final JScrollPane jScrollPaneException;
    private final JTextArea jTextAreaException;

    public ErrorDialog(String errorMessage) {
        this(errorMessage, null);
    }

    public ErrorDialog(String errorMessage, Throwable exception) {
        this.setTitle("Error");
        this.setModal(true);
        this.setResizable(false);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        jPanelTop = new JPanel();
        jPanelTop.setLayout(null);
        jPanelTop.setPreferredSize(new Dimension(480, 100));

        jTextPaneErrorMsg = new JTextPane();
        jTextPaneErrorMsg.setFont(jTextPaneErrorMsg.getFont().deriveFont(
                jTextPaneErrorMsg.getFont().getStyle() | Font.BOLD,
                jTextPaneErrorMsg.getFont().getSize() + 1));
        jTextPaneErrorMsg.setBorder(null);
        jTextPaneErrorMsg.setEditable(false);
        jTextPaneErrorMsg.setBackground(null);
        jScrollPaneErrorMsg = new JScrollPane(jTextPaneErrorMsg);
        jScrollPaneErrorMsg.setBorder(null);
        jScrollPaneErrorMsg.setSize(new Dimension(405, 80));
        jScrollPaneErrorMsg.setLocation(new Point(71, 13));
        jPanelTop.add(jScrollPaneErrorMsg);

        jPanelCenter = new JPanel();
        jPanelCenter.setSize(new Dimension(420, 300));
        jTextAreaException = new JTextArea();
        jScrollPaneException = new JScrollPane(jTextAreaException);
        jScrollPaneException.setPreferredSize(new Dimension(470, 300));
        jPanelCenter.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        jPanelCenter.add(jScrollPaneException);

        jPanelBottom = new JPanel();
        jButtonShowHideDetails = new JButton();
        jButtonClose = new JButton();
        jPanelBottom.setLayout(new FlowLayout(FlowLayout.CENTER, 30, 15));

        jButtonShowHideDetails.setText(SHOW_DETAILS_TEXT);
        jButtonShowHideDetails.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                showHideExceptionDetails();
            }
        });
        jPanelBottom.add(jButtonShowHideDetails);

        jButtonClose.setText("Close");
        jButtonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dispose();
            }
        });
        jPanelBottom.add(jButtonClose);

        this.setLayout(new BorderLayout());
        this.add(jPanelTop, BorderLayout.NORTH);
        this.add(jPanelCenter, BorderLayout.CENTER);
        this.add(jPanelBottom, BorderLayout.SOUTH);

        this.jTextPaneErrorMsg.setEditorKit(new VerticalCenteredEditorKit());
        this.jTextPaneErrorMsg.setText(errorMessage);

        this.jPanelCenter.setVisible(false);

        if (exception != null) {
            String exceptionText = getStackTraceAsString(exception);
            jTextAreaException.setText(exceptionText);
            jTextAreaException.setEditable(false);
        } else {
            this.jButtonShowHideDetails.setVisible(false);
        }

        // Make [Escape] key as close button
        this.registerEscapeKey();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setModal(true);
        this.pack();
        centerDialogOnTheScreen();
    }

    public static void showError(String errorMessage,
                                 Throwable throwable) {
        ErrorDialog errorDialog =
                new ErrorDialog(errorMessage, throwable);
        errorDialog.setVisible(true);
    }

    public static void showError(String errorMessage) {
        ErrorDialog.showError(errorMessage, null);
    }

    public static void main(String[] args) {
        ErrorDialog.showError("This is an error message.", new Exception());
    }

    private void centerDialogOnTheScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension dialogSize = this.getSize();
        int centerPosX = (screenSize.width - dialogSize.width) / 2;
        int centerPosY = (screenSize.height - dialogSize.height) / 2;
        setLocation(centerPosX, centerPosY);
    }

    private void showHideExceptionDetails() {
        if (this.jPanelCenter.isVisible()) {
            // Hide the exception details
            this.jButtonShowHideDetails.setText(SHOW_DETAILS_TEXT);
            this.jPanelCenter.setVisible(false);
            this.pack();
            centerDialogOnTheScreen();
        } else {
            // Show the exception details
            this.jButtonShowHideDetails.setText(HIDE_DETAILS_TEXT);
            this.jPanelCenter.setVisible(true);
            this.pack();
            centerDialogOnTheScreen();
        }
    }

    private String getStackTraceAsString(Throwable exception) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        exception.printStackTrace(printWriter);
        return result.toString();
    }

    /**
     * Make the [Escape] key to behave like the [Close] button.
     */
    public void registerEscapeKey() {
        KeyStroke escapeKeyStroke =
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action escapeAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                jButtonClose.doClick();
            }
        };

        this.rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                escapeKeyStroke, "ESCAPE");
        this.rootPane.getActionMap().put("ESCAPE", escapeAction);
    }

    public void hideAndDisposeDialog() {
        this.setVisible(false);
        this.dispose();
    }
}
 
