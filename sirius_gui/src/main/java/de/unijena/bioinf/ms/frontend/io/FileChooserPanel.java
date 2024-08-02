/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.io;

import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.utils.PlaceholderTextField;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FileChooserPanel extends JPanel {
    public final PlaceholderTextField field = new PlaceholderTextField(20);
    public final JButton changeDir = Buttons.getFileChooserButton16("Choose file or directory");

    public FileChooserPanel() {
        this("", JFileChooser.FILES_AND_DIRECTORIES, JFileChooser.OPEN_DIALOG);
    }

    public FileChooserPanel(String currentPath) {
        this(currentPath, JFileChooser.FILES_AND_DIRECTORIES, JFileChooser.OPEN_DIALOG);
    }

    public FileChooserPanel(int fileChooserMode) {
        this("", fileChooserMode, JFileChooser.OPEN_DIALOG);
    }

    public FileChooserPanel(int fileChooserMode, int dialogMode) {
        this("", fileChooserMode, dialogMode);
    }

    public FileChooserPanel(String currentPath, int fileChooserMode) {
        this(currentPath, currentPath, fileChooserMode, JFileChooser.OPEN_DIALOG);

    }

    public FileChooserPanel(String currentPath, int fileChooserMode, int dialogMode) {
        this(currentPath, currentPath, fileChooserMode, dialogMode);
    }

    public FileChooserPanel(String currentPathChooser, String currentPathTextField, int fileChooserMode, int dialogMode) {

        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));


        Path current = Path.of(currentPathChooser);
        if (!Files.isDirectory(current))
            current = current.getParent();

        final JFileChooser fileChooser = new JFileChooser(current.toFile());
        fileChooser.setDialogType(dialogMode);
        fileChooser.setFileSelectionMode(fileChooserMode);

        switch (fileChooserMode) {
            case JFileChooser.FILES_ONLY -> changeDir.setToolTipText("Choose a file");
            case JFileChooser.DIRECTORIES_ONLY -> changeDir.setToolTipText("Choose a directory");
            default -> {}
        }

        field.setText(currentPathTextField);

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
        String it = field.getText();
        if (it == null || it.isBlank())
            return null;
        return it;
    }

    @Override
    public void setToolTipText(String text) {
        super.setToolTipText(text);
        field.setToolTipText(text);
        changeDir.setToolTipText(text);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        field.setEnabled(enabled);
        changeDir.setEnabled(enabled);
    }

    public static void test_main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                ex.printStackTrace();
            }
            JFrame frame = new JFrame("Testing");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new FileChooserPanel(System.getProperty("user.home"), 1));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
