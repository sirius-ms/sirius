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

package de.unijena.bioinf.ms.gui.logging;


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

