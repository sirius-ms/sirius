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

package de.unijena.bioinf.ms.gui.logging;


import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingPanel extends JPanel {
    JComboBox<String> levelBox;
    TextAreaHandler handler;

    public LoggingPanel(TextAreaHandler handler) {
        setLayout(new BorderLayout());

        this.handler = handler;
        handler.getArea().setEditable(false);


        this.levelBox = new JComboBox<>(new String[]{"OFF", "SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST", "ALL"});
        this.levelBox.setSelectedItem(handler.getLevel().getName());
        this.levelBox.addActionListener(event -> {
            Level l = Level.parse((String) levelBox.getModel().getSelectedItem());
            if (handler.getLevel() != l)
                handler.setLevel(l);
        });

        JPanel levelPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        levelPane.add(new JLabel("Log Level"));
        levelPane.add(levelBox);

        JButton button = new JButton("Clear");
        button.addActionListener(e -> handler.getArea().setText(""));
        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPane.add(button);

        JButton copyCommand = new JButton("Copy Log");
        copyCommand.setToolTipText("Copy Log to clipboard.");
        copyCommand.addActionListener(evt -> {
            StringSelection stringSelection = new StringSelection(handler.getArea().getText());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        });
        buttonPane.add(copyCommand);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPane, BorderLayout.EAST);
        southPanel.add(levelPane, BorderLayout.WEST);

        add(new JScrollPane(handler.getArea()), BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(640, 480));
    }


    public static void test_main(String[] args) {
        final Logger logger = Logger.getLogger("testlogger");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        JTextArea textArea = new JTextArea();
        TextAreaHandler h = new TextAreaHandler(textArea, Level.ALL);
        logger.addHandler(h);

        LoggingPanel logPane = new LoggingPanel(h);

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

