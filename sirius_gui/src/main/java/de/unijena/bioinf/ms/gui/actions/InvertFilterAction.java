package de.unijena.bioinf.ms.gui.actions;/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2021 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

import de.unijena.bioinf.ms.gui.SiriusGui;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * this allows to invert the compound list filter.
 */
public class InvertFilterAction extends AbstractGuiAction {

    public InvertFilterAction(SiriusGui gui) {
        super("Invert filter", gui);
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        mainFrame.getCompoundList().toggleInvertFilter();
        if (mainFrame.getCompoundList().isFilterInverted()) {
            this.putValue(Action.NAME, "Revert filter");
        } else {
            this.putValue(Action.NAME, "Invert filter");
        }
    }
}
