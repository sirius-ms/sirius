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

package de.unijena.bioinf.ms.gui.fingerid.fingerprints;

import ca.odell.glazedlists.gui.TableFormat;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by kaidu on 22.05.17.
 */
public class FingerprintTableFormat implements TableFormat<FingerIdPropertyBean> {
    protected FingerprintTable table;
    protected static String[] columns = new String[]{
            "Index", "SMARTS", "Posterior Probability", "#Atoms", "Type", "Positive training examples", "Predictor quality (F1)"
    };

    public FingerprintTableFormat(FingerprintTable table) {
        this.table = table;
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getColumnValue(FingerIdPropertyBean m, int column) {
        switch (column) {
            case 0: return m.getAbsoluteIndex();
            case 1: return m.getMolecularProperty().getDescription();
            case 2: return m.getProbability();
            case 3: return m.getMatchSizeDescription();
            case 4: return m.getFingerprintTypeName();
            case 5: return m.getNumberOfTrainingExamples();
            case 6: return m.getFScore();
            default:return null;
        }
    }
}
