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

package de.unijena.bioinf.ms.gui.molecular_formular;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 25.01.17.
 */

import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.ms.gui.table.SiriusTableFormat;
import de.unijena.bioinf.ms.gui.table.list_stats.ListStats;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;

import java.util.function.Function;

/**
 * Display issues in a tabular form.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusResultTableFormat extends SiriusTableFormat<FormulaResultBean> {
    private static final int COL_COUNT = 10;

    protected SiriusResultTableFormat(Function<FormulaResultBean,Boolean> isBest) {
        super(isBest);
    }

    @Override
    public int highlightColumnIndex() {
        return COL_COUNT;
    }

    @Override
    public int getColumnCount() {
        return COL_COUNT;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Rank";
            case 1:
                return "Molecular Formula";
            case 2:
                return "Adduct";
            case 3:
                return "Zodiac Score";
            case 4:
                return "Sirius Score";
            case 5:
                return "Isotope Score";
            case 6:
                return "Tree Score";
            case 7:
                return "Explained Peaks";
            case 8:
                return "Total Explained Intensity";
            case 9:
                return "Median Mass Error (ppm)";
            case 10:
                return "Best";
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public Object getColumnValue(FormulaResultBean result, int column) {
        switch (column) {
            case 0:
                return result.getRank();
            case 1:
                return result.getMolecularFormula().toString();
            case 2:
                return result.getPrecursorIonType().toString();
            case 3:
                return result.getScoreValue(ZodiacScore.class);
            case 4:
                return result.getScoreValue(SiriusScore.class);
            case 5:
                return result.getScoreValue(IsotopeScore.class);
            case 6:
                return result.getScoreValue(TreeScore.class);
            case 7:
                return result.getNumOfExplainedPeaks();
            case 8:
                return result.getExplainedIntensityRatio();
            case 9:
                return result.getMedianMassDevPPM();
            case 10:
                return isBest.apply(result);
            default:
                throw new IllegalStateException();
        }
    }
}

