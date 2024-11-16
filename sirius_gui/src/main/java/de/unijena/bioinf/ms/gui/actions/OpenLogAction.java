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

package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.logging.LogDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.logging.Level;

public class OpenLogAction extends AbstractAction {

    private final LogDialog source;

    public OpenLogAction() {
        super("Log");
        putValue(Action.LARGE_ICON_KEY, Icons.LOG.derive(32,32));
        putValue(Action.SHORT_DESCRIPTION, "Show SIRIUS logs in Popup Dialog.");
        source = new LogDialog(false, Level.INFO); //todo property for log level;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() instanceof JComponent c)
            source.setLocationRelativeTo(c.getRootPane());
        source.setVisible(true);
    }
}
