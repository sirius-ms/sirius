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

package de.unijena.bioinf.ms.gui.mainframe.instance_panel;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;

import javax.swing.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class CompoundContextMenu extends JPopupMenu {

    public CompoundContextMenu(SiriusGui gui) {
        add(new JMenuItem(SiriusActions.COMPUTE.getInstance(gui, true)));
        add(new JMenuItem(SiriusActions.SUMMARIZE_EXP.getInstance(gui, true)));
        add(new JMenuItem(SiriusActions.DELETE_EXP.getInstance(gui, true)));
        addSeparator();
//        add(new JMenuItem(SiriusActions.EDIT_EXP.getInstance(gui, true))); //todo nightsky: enable edit if needed.
        add(new JMenuItem(SiriusActions.CHANGE_ADDCUCT_EXP.getInstance(gui, true)));
        addSeparator();
        add(new JMenuItem(SiriusActions.ORDER_BY_RT.getInstance(gui, true)));
        add(new JMenuItem(SiriusActions.ORDER_BY_MASS.getInstance(gui, true)));
        add(new JMenuItem(SiriusActions.ORDER_BY_NAME.getInstance(gui, true)));
        add(new JMenuItem(SiriusActions.ORDER_BY_ID.getInstance(gui, true)));
        add(new JMenuItem(SiriusActions.ORDER_BY_QUALITY.getInstance(gui, true)));
        add(new JMenuItem(SiriusActions.ORDER_BY_CONFIDENCE.getInstance(gui, true)));
        addSeparator();
        add(new JMenuItem(SiriusActions.RESET_FILTER.getInstance(gui, true)));

        add(new JMenuItem(SiriusActions.TOOGLE_CONFIDENCE_MODE.getInstance(gui, true)));

        addSeparator();
        add(new JMenuItem(SiriusActions.COPY_ID.getInstance(gui, true)));
    }
}
