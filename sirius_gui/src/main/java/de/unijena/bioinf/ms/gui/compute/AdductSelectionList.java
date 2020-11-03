/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel to select Adducts e.g. for CSI:FingerID, Based on a given list of Ionizations.
 * Provides: AdductSettings
 */
public class AdductSelectionList extends JCheckBoxList<String> implements ListSelectionListener {

    private final JCheckBoxList<String> source;

    public AdductSelectionList(JCheckBoxList<String> sourceIonization) {
        super();
        source = sourceIonization;
        change();
        source.addListSelectionListener(this);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        change();
    }

    private void change() {
        boolean enabled = isEnabled();
        List<String> m = new ArrayList<>();
        for (String ionisation : source.getCheckedItems()) {
            for (PrecursorIonType adduct : PeriodicTable.getInstance().adductsByIonisation(PrecursorIonType.getPrecursorIonType(ionisation))) {
                m.add(adduct.toString());
            }
        }
        replaceElements(m);
        checkAll(source.getCheckedItems());
        setEnabled(enabled);
        firePropertyChange("refresh", null, null);
    }

    @Override
    public void setEnabled(boolean enabled) {
        boolean old = isEnabled();
        super.setEnabled(enabled);
        firePropertyChange("refresh", old, isEnabled());
    }
}
