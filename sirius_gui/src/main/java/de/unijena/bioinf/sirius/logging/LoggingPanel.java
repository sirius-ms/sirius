package de.unijena.bioinf.sirius.logging;


import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingPanel extends JPanel {

    public LoggingPanel(JTextArea textArea) {
        setLayout(new BorderLayout());

        textArea.setEditable(false);

        JButton button = new JButton("Clear");
        button.addActionListener(e -> textArea.setText(""));

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(button);

        add(new JScrollPane(textArea), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(320, 240));
    }


    public static void main(String[] args) {
        final Logger logger = Logger.getLogger("testlogger");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        JTextArea textArea = new JTextArea();
        OutputStream os = new TextAreaOutputStream(textArea);
        logger.addHandler(new TextAreaHandler(os, Level.ALL));

        LoggingPanel logPane = new LoggingPanel(textArea);

        logger.info("test, TEST");


        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException
                    | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                ex.printStackTrace();
            }
            JFrame frame = new JFrame("TextAreaOutputStream");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.getContentPane().add(logPane);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        logger.info("test, TEST");
        logger.info("test, TEST");
        logger.info("test, TEST");
        logger.info("test, TEST");
        logger.info("test, TEST");
        logger.info("test, TEST");
        logger.info("test, TEST");
        logger.info("test, TEST");
        logger.info("test, TEST");
        logger.info("test, TEST");
        logger.info("test, TEST");
        logger.info("test, TEST");

    }
}

