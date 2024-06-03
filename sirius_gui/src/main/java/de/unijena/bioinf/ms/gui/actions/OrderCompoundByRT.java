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

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ms.gui.SiriusGui;

import java.awt.event.ActionEvent;

public class OrderCompoundByRT extends AbstractGuiAction {

    public OrderCompoundByRT(SiriusGui gui) {
        super("Order by RT (default)", gui);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        mainFrame.getCompoundList().orderBy((o1, o2) -> {
            double d1 = o1.getRT().map(RetentionTime::getMiddleTime).orElse(Double.NaN);
            double d2 = o2.getRT().map(RetentionTime::getMiddleTime).orElse(Double.NaN);
            return Double.compare(d1, d2);
        });
    }
}
