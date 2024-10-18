/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.fingerid;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class CandidateListTableHeaderRenderer extends DefaultTableCellRenderer {

    private final static String[] toolTips = new String[]{
            "Ranked by CSI:FingerID score",
            null,
            null,
            null,
            null,
            "CSI:FingerID score between the database structure's fingerprint and the predicted fingerprint from the input data",
            "Tanimoto similarity between the database structure's fingerprint and the predicted fingerprint from the input data",
            "Octanol/water partition coefficient",
            null,
            "Lipid class predicted by El Gordo",
            "If marked, this structure is present in a structure database",
            "If marked, this structure was generated de novo by MSNovelist"
    };

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (column < toolTips.length) {
            this.setToolTipText(toolTips[column]);
        }
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

}
