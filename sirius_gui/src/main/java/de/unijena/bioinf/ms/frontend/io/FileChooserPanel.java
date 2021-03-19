/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.io;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
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
    public final JTextField field = new JTextField();
    public final JButton changeDir = Buttons.getFileChooserButton16("Choose file or directory");

    public FileChooserPanel() {
        this("", 2, JFileChooser.OPEN_DIALOG);
    }

    public FileChooserPanel(String currentPath) {
        this(currentPath, 2, JFileChooser.OPEN_DIALOG);
    }

    public FileChooserPanel(int fileChooserMode) {
        this("", fileChooserMode, JFileChooser.OPEN_DIALOG);
    }
    public FileChooserPanel(int fileChooserMode, int dialogMode) {
        this("", fileChooserMode, dialogMode);
    }

    public FileChooserPanel(String currentPath, int fileChooserMode) {
        this(currentPath,fileChooserMode, JFileChooser.OPEN_DIALOG);

    }
    public FileChooserPanel(String currentPath, int fileChooserMode, int dialogMode) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));


        final JFileChooser fileChooser = new JFileChooser(currentPath);
        fileChooser.setDialogType(dialogMode);
        fileChooser.setFileSelectionMode(fileChooserMode);

        field.setText(currentPath);
        add(field);
        add(changeDir);
        changeDir.addActionListener(e -> {
            final int r = fileChooser.showOpenDialog(FileChooserPanel.this);
            if (r == JFileChooser.APPROVE_OPTION) {
                final File file = fileChooser.getSelectedFile();
                if (file != null) {
                    field.setText(file.toString());
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
        EventQueue.invokeLater(() -> {
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
        });
    }
}
