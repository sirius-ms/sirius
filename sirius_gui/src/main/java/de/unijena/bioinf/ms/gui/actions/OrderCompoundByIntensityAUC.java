/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.projectspace.InstanceBean;

import java.awt.event.ActionEvent;
import java.util.Comparator;

/**
 * Orders the compound list by MS1 peak area (area under the chromatographic peak), largest first; features
 * without a known area come last.
 */
public class OrderCompoundByIntensityAUC extends AbstractGuiAction {

    public OrderCompoundByIntensityAUC(SiriusGui gui) {
        super("Peak area (AUC)", gui);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        mainFrame.getFilterableCompoundListPanel().runInBackgroundAndLoad(() ->
                mainFrame.getCompoundList().orderBy(Comparator.comparing(InstanceBean::getIntensityAUC,
                        Comparator.nullsLast(Comparator.reverseOrder()))));
    }
}
