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

import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;

import javax.swing.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class CompoundContextMenu extends JPopupMenu {

    public CompoundContextMenu(MainFrame mf) {
        add(new JMenuItem(SiriusActions.COMPUTE.getInstance(mf, true, getActionMap())));
        add(new JMenuItem(SiriusActions.SUMMARIZE_EXP.getInstance(mf, true, getActionMap())));
        add(new JMenuItem(SiriusActions.DELETE_EXP.getInstance(mf, true, getActionMap())));
        addSeparator();
        add(new JMenuItem(SiriusActions.EDIT_EXP.getInstance(mf, true, getActionMap())));
        add(new JMenuItem(SiriusActions.REMOVE_FORMULA_EXP.getInstance(mf, true, getActionMap())));
        add(new JMenuItem(SiriusActions.CHANGE_ADDCUCT_EXP.getInstance(mf, true, getActionMap())));
        addSeparator();
        add(new JMenuItem(SiriusActions.ORDER_BY_INDEX.getInstance(mf, true, getActionMap())));
        add(new JMenuItem(SiriusActions.ORDER_BY_RT.getInstance(mf, true, getActionMap())));
        add(new JMenuItem(SiriusActions.ORDER_BY_MASS.getInstance(mf, true, getActionMap())));
        add(new JMenuItem(SiriusActions.ORDER_BY_NAME.getInstance(mf, true, getActionMap())));
        add(new JMenuItem(SiriusActions.ORDER_BY_CONFIDENCE.getInstance(mf, true, getActionMap())));
        addSeparator();
        add(new JMenuItem(SiriusActions.RESET_FILTER.getInstance(mf, true, getActionMap())));
    }
}
