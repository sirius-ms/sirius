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

package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.ms.gui.table.SiriusTableFormat;
import de.unijena.bioinf.ms.gui.table.list_stats.ListStats;

import java.util.function.Function;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateTableFormat extends SiriusTableFormat<FingerprintCandidateBean> {
    protected CandidateTableFormat(Function<FingerprintCandidateBean, Boolean> isBest) {
        super(isBest);
    }

    protected static String[] columns = new String[]{
            "Rank",
            "Name",
            "SMILES",
            "Molecular Formula",
            "Adduct",
            "CSI:FingerID Score",
            "Similarity",
            "#PubMed IDs",
            "XLogP",
            "InChIKey",
            "Best"
    };

    @Override
    public int highlightColumnIndex() {
        return columns.length - 1;
    }


    public int getColumnCount() {
        return highlightColumnIndex();
    }

    public String getColumnName(int column) {
        return columns[column];
    }

    public Object getColumnValue(FingerprintCandidateBean result, int column) {
        int col = 0;
        if (column == col++) return result.rank;
        if (column == col++) return result.candidate.getName() != null ? result.candidate.getName() : "";
        if (column == col++) return result.candidate.getSmiles();
        if (column == col++) return result.getMolecularFormula();
        if (column == col++) return result.adduct.toString();
        if (column == col++) return result.getScore();
        if (column == col++) return result.getTanimotoScore();
        if (column == col++) return result.getFingerprintCandidate().getPubmedIDs();
        if (column == col++) return result.candidate.getXlogp();
        if (column == col++) return result.candidate.getInchi().key;
        if (column == col) return isBest.apply(result);

        throw new IllegalStateException();
    }
}
