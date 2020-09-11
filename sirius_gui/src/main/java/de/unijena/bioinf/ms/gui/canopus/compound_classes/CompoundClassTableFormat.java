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

package de.unijena.bioinf.ms.gui.canopus.compound_classes;

import ca.odell.glazedlists.gui.TableFormat;
import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;

public class CompoundClassTableFormat implements TableFormat<ClassyfirePropertyBean> {
    protected static String[] columns = new String[]{
            "Index",
            "Name",
            "Posterior Probability",
            "Description",
            "ID",
            "Parent",
//            "Positive training examples",
//            "Predictor quality (F1)"
    };

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getColumnValue(ClassyfirePropertyBean prop, int column) {
        int col = 0;
        if (column == col++) return prop.getAbsoluteIndex();
        if (column == col++) return prop.getMolecularProperty().getName();
        if (column == col++) return prop.getProbability();
        if (column == col++) return prop.getMolecularProperty().getDescription();
        if (column == col++) return prop.getMolecularProperty().getChemontIdentifier();
        final ClassyfireProperty parent = prop.getMolecularProperty().getParent();
        if (column == col++) return parent!=null ? parent.getName() : "";
//        if (column == col++) return prop.getNumberOfTrainingExamples();
//        if (column == col) return prop.getFScore();
        return null;
    }
}
