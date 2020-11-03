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

package de.unijena.bioinf.ms.gui.utils;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import javax.swing.*;
import java.util.Comparator;
import java.util.Vector;
import java.util.stream.Collectors;

public class PrecursorIonTypeSelector extends JComboBox<String> {
    public static final String name = "Adduct";

    public PrecursorIonTypeSelector(Vector<String> adducts, String selected) {
        setModel(new DefaultComboBoxModel<>(adducts));
        setSelectedItem(selected);
    }

    public PrecursorIonTypeSelector(String selected) {
        this(new Vector<>(PeriodicTable.getInstance().getAdductsAndUnKnowns().stream().sorted(new PrecursorViewComparator().reversed()).map(PrecursorIonType::toString).collect(Collectors.toList())), selected);
    }

    public PrecursorIonTypeSelector() {
        this(new Vector<>(PeriodicTable.getInstance().getAdductsAndUnKnowns().stream().sorted(new PrecursorViewComparator().reversed()).map(PrecursorIonType::toString).collect(Collectors.toList())), PeriodicTable.getInstance().unknownPositivePrecursorIonType().getIonization().getName());
    }

    public void refresh() {
        setModel(new DefaultComboBoxModel<>(new Vector<>(PeriodicTable.getInstance().getAdductsAndUnKnowns().stream().sorted(new PrecursorViewComparator().reversed()).map(PrecursorIonType::toString).collect(Collectors.toList()))));
    }

    static class PrecursorViewComparator implements Comparator<PrecursorIonType> {

        @Override
        public int compare(PrecursorIonType o1, PrecursorIonType o2) {
            int r = Boolean.compare(o1.isIonizationUnknown(), o2.isIonizationUnknown());
            if (r == 0) {
                r = Boolean.compare(o1.getCharge() > 0, o2.getCharge() > 0);
                if (r == 0) {
                    r = o1.toString().compareTo(o2.toString());
                }
            }
            return r;
        }
    }
}

