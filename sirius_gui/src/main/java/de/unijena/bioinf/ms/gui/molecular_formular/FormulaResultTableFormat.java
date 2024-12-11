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

import de.unijena.bioinf.ms.gui.table.SiriusTableFormat;
import io.sirius.ms.sdk.model.Deviation;
import io.sirius.ms.sdk.model.LipidAnnotation;
import de.unijena.bioinf.projectspace.FormulaResultBean;

import java.util.function.Function;

/**
 * Display FomulaResultBean in a tabular form.
 *
 * @author Markus Fleischauer
 */
public class FormulaResultTableFormat extends SiriusTableFormat<FormulaResultBean> {
    private final Function<FormulaResultBean, Double> fallBackNormalizedSiriusScoreFunction;

    protected FormulaResultTableFormat(Function<FormulaResultBean, Boolean> isBest, Function<FormulaResultBean, Double> fallBackNormalizedSiriusScoreFunction) {
        super(isBest);
        this.fallBackNormalizedSiriusScoreFunction = fallBackNormalizedSiriusScoreFunction;
    }

    @Override
    public int highlightColumnIndex() {
        return getColumnCount();
    }

    @Override
    public int getColumnCount() {
        return columns.length - 1;
    }

    protected static String[] columns = new String[]{
            "Rank",
            "Molecular Formula",
            "Adduct",
            "Zodiac Score",
            "Sirius Score (normalized)",
            "Isotope Score",
            "Tree Score",
            "Explained Peaks",
            "Total Explained Intensity",
            "Median Mass Error (ppm)",
            "Median Mass Error (mDa)",
            "Lipid Class",
            "Best",
    };

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getColumnValue(FormulaResultBean result, int column) {
        int col = 0;
        if (column == col++) return result.getRank().orElse(null);
        else if (column == col++) return result.getMolecularFormula();
        else if (column == col++) return result.getAdduct();
        else if (column == col++) return result.getZodiacScore().orElse(Double.NaN);
        else if (column == col++) return result.getSiriusScoreNormalized().orElseGet(() -> fallBackNormalizedSiriusScoreFunction.apply(result));
        else if (column == col++) return result.getIsotopeScore().orElse(Double.NaN);
        else if (column == col++) return result.getTreeScore().orElse(Double.NaN);
        else if (column == col++) return result.getNumOfExplainedPeaks().stream().mapToDouble(v -> (double) v).findFirst().orElse(Double.NaN);
        else if (column == col++) return result.getTotalExplainedIntensity().orElse(Double.NaN);
        else if (column == col++) return result.getMedianMassDeviation().map(Deviation::getPpm).orElse(Double.NaN);
        else if (column == col++) return result.getMedianMassDeviation().map(Deviation::getAbsolute).map(d -> d * 1000d).orElse(Double.NaN);
        else if (column == col++) return result.getLipidAnnotation().map(LipidAnnotation::getLipidSpecies).orElse(""); //N/A or better empty?
        else if (column == col++) return isBest.apply(result);

        throw new IllegalStateException();
    }
}

