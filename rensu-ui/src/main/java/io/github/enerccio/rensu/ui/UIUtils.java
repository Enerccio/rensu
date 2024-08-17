package io.github.enerccio.rensu.ui;

import org.slf4j.Logger;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.PrintWriter;
import java.io.StringWriter;

public class UIUtils {

    public static void onError(JFrame parentComponent, Logger logger, Throwable t, String m) {
        logger.error(t.getMessage());
        logger.debug(m, t);

        StringWriter stringWriter = new StringWriter();
        t.printStackTrace(new PrintWriter(stringWriter));

        JLabel message = new JLabel(m);
        message.setBorder(BorderFactory.createEmptyBorder(3, 0, 10, 0));

        JTextArea text = new JTextArea();
        text.setEditable(false);
        text.setFont(UIManager.getFont("Label.font"));
        text.setText(stringWriter.toString());
        text.setCaretPosition(0);

        JScrollPane scroller = new JScrollPane(text);
        scroller.setPreferredSize(new Dimension(400, 200));

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        panel.add(message, BorderLayout.NORTH);
        panel.add(scroller, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(parentComponent, panel, "Error",
                JOptionPane.ERROR_MESSAGE);
    }

}
