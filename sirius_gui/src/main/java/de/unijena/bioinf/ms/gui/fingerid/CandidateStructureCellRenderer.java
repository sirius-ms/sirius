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

package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.ms.gui.configs.Colors;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;

import javax.swing.*;
import java.awt.*;

/**
 * Created by fleisch on 24.05.17.
 */
public class CandidateStructureCellRenderer implements ListCellRenderer<FingerprintCandidateBean> {
    private CompoundStructureImage image = new CompoundStructureImage(StandardGenerator.HighlightStyle.None);

    @Override
    public Component getListCellRendererComponent(JList<? extends FingerprintCandidateBean> list, FingerprintCandidateBean value, int index, boolean isSelected, boolean cellHasFocus) {
        JPanel nu =  new JPanel();
        image.molecule = value;
        image.backgroundColor = (index % 2 == 0 ? Colors.CellsAndRows.LargerCells.ALTERNATING_CELL_1 : Colors.CellsAndRows.LargerCells.ALTERNATING_CELL_1);
        nu.setBackground(image.backgroundColor);
        nu.add(image);
        return nu;
    }
}
