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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.ms.gui.table.SiriusTableFormat;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;

import java.util.function.Function;

/**
 * Display issues in a tabular form.
 *
 * @author Markus Fleischauer
 */
public class SiriusResultTableFormat extends SiriusTableFormat<FormulaResultBean> {
    private static final int COL_COUNT = 13;

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
                return "Precursor Molecular Formula";
            case 2:
                return "Molecular Formula";
            case 3:
                return "Adduct";
            case 4:
                return "Zodiac Score";
            case 5:
                return "Sirius Score";
            case 6:
                return "Isotope Score";
            case 7:
                return "Tree Score";
            case 8:
                return "Explained Peaks";
            case 9:
                return "Total Explained Intensity";
            case 10:
                return "Median Mass Error (ppm)";
            case 11:
                return "Median Absolute Mass Error (ppm)";
            case 12:
                return "Lipid Class";
            case 13:
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
                return result.getPrecursorMolecularFormula().toString();
            case 2:
                return result.getMolecularFormula().toString();
            case 3:
                return result.getPrecursorIonType().toString();
            case 4:
                return result.getScoreValue(ZodiacScore.class);
            case 5:
                return result.getScoreValue(SiriusScore.class);
            case 6:
                return result.getScoreValue(IsotopeScore.class);
            case 7:
                return result.getScoreValue(TreeScore.class);
            case 8:
                return result.getNumOfExplainedPeaks();
            case 9:
                return result.getExplainedIntensityRatio();
            case 10:
                return result.getMedianMassDevPPM();
            case 11:
                return result.getMedianAbsoluteMassDevPPM();
            case 12:
                return result.getFragTree().flatMap(t -> t.getAnnotation(LipidSpecies.class)).filter(ls -> ls.getHypotheticalMolecularFormula().orElse(MolecularFormula.emptyFormula()).equals(result.getMolecularFormula())).map(LipidSpecies::toString).orElse("None"); //annotate if same MF or Lipid MF unknown //todo  But I am not sure if Lipid annotation is even present, if MF is unknown
            case 13:
                return isBest.apply(result);
            default:
                throw new IllegalStateException();
        }
    }
}

