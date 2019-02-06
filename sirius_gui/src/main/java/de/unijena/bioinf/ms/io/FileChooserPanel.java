package de.unijena.bioinf.ms.io;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 07.10.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.configs.Buttons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FileChooserPanel extends JPanel {
    final JTextField field = new JTextField();
    final JButton changeDir = Buttons.getFileChooserButton16("Choose file or directory");

    public FileChooserPanel() {
        this("", 2);
    }

    public FileChooserPanel(String currentPath) {
        this(currentPath, 2);
    }

    public FileChooserPanel(int fileChooserMode) {
        this("", fileChooserMode);
    }

    public FileChooserPanel(String currentPath, int fileChooserMode) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));


        final JFileChooser fileChooser = new JFileChooser(currentPath);
        fileChooser.setFileSelectionMode(fileChooserMode);
        field.setText(currentPath);
        add(field);
        add(changeDir);
        changeDir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int r = fileChooser.showOpenDialog(FileChooserPanel.this);
                if (r == JFileChooser.APPROVE_OPTION) {
                    final File file = fileChooser.getSelectedFile();
                    if (file != null && file.isDirectory()) {
                        field.setText(file.toString());
                    }
                }
            }
        });
    }

    public String getFilePath() {
        return field.getText();
    }

    @Override
    public void setToolTipText(String text) {
        super.setToolTipText(text);
        field.setToolTipText(text);
        changeDir.setToolTipText(text);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    ex.printStackTrace();
                }

                String s = ApplicationCore.VERSION_STRING();
                JFrame frame = new JFrame("Testing");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new FileChooserPanel(System.getProperty("user.home"), 1));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
