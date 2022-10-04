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


import de.unijena.bioinf.ms.gui.mainframe.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class LogDialog extends JDialog {
    private final LoggingPanel lPanel;
    private final TextAreaHandler handler;

    public LogDialog(Frame owner, boolean modal, Level loglevel, Logger... loggers) {
        super(owner, "Log", modal);

        JTextArea textArea = new JTextArea();
        this.handler = new TextAreaHandler(textArea, loglevel);

        if (loggers == null || loggers.length == 0) {
            LogManager.getLogManager().getLoggerNames().asIterator().forEachRemaining(name -> {
                if (LogManager.getLogManager().getLogger(name) != null)
                    LogManager.getLogManager().getLogger(name).addHandler(handler);
            });
        } else {
            Arrays.stream(loggers).forEach(it -> it.addHandler(handler));
        }


        lPanel = new LoggingPanel(handler);
        add(lPanel);

        pack();
        setLocationRelativeTo(getParent());
    }

    @Override
    public void setVisible(boolean b) {
        if (b){
            setLocationRelativeTo(MainFrame.MF);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setPreferredSize(new Dimension(Math.min(screenSize.width, MainFrame.MF.getWidth()), Math.min(400, screenSize.height)));
            pack();
        }
        super.setVisible(b);
    }
}
