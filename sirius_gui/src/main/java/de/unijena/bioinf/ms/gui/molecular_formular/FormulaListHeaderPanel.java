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

package de.unijena.bioinf.ms.gui.molecular_formular;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfoStore;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaListHeaderPanel extends JPanel {
    public FormulaListHeaderPanel(FormulaList table, JComponent center) {
        this(table,center,false);
    }

    public FormulaListHeaderPanel(FormulaList formulaList, JComponent center, boolean detailed) {
        super(new BorderLayout());
        if (center instanceof ActiveElementChangedListener)
            formulaList.addActiveResultChangedListener((ActiveElementChangedListener) center);

        if (detailed){
            add(new FormulaListDetailView(formulaList),BorderLayout.NORTH);
        }else{
            FormulaListCompactView formulaListCompactView = new FormulaListCompactView(formulaList);
            add(formulaListCompactView,BorderLayout.NORTH);
            formulaListCompactView.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.Canopus_Formulas);
            formulaListCompactView.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, SoftwareTourInfoStore.Fingerprint_Formulas);
        }
        add(center,BorderLayout.CENTER);


    }
}
