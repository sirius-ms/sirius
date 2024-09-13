/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.frontend.subtools.summaries;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class NoSqlFormulaSummaryWriter extends SummaryTable {

    final static List<String> HEADER = List.of(
            "formulaRank",
            "molecularFormula",
            "adduct",
            "precursorFormula",
            "ZodiacScore",
            "SiriusScore",
            "TreeScore",
            "IsotopeScore",
            "numExplainedPeaks",
            "explainedIntensity",

            "medianMassErrorFragmentPeaks(ppm)",
            "medianAbsoluteMassErrorFragmentPeaks(ppm)",

            "massErrorPrecursor(ppm)",

            "lipidClass",
            // metadata for mapping
            "ionMass",
            "retentionTimeInSeconds",
            "retentionTimeInMinutes",
            "formulaId",
            "alignedFeatureId",
            "mappingFeatureId",
            "overallFeatureQuality");


    public NoSqlFormulaSummaryWriter(SummaryTableWriter writer) {
        super(writer);
    }

    public void writeHeader() throws IOException {
        writer.writeHeader(HEADER);
    }

    public void writeFormulaCandidate(AlignedFeatures f, FormulaCandidate fc, FTree tree) throws IOException {
        List<Object> row = new ArrayList<>();

        FTreeMetricsHelper scores = new FTreeMetricsHelper(tree);
        row.add(fc.getFormulaRank());
        row.add(fc.getMolecularFormula().toString());
        row.add(fc.getAdduct().toString());
        row.add(fc.getPrecursorFormulaWithCharge());
        row.add(fc.getZodiacScore());
        row.add(fc.getSiriusScore());
        row.add(fc.getTreeScore());
        row.add(fc.getIsotopeScore());
        row.add(scores.getNumOfExplainedPeaks());
        row.add(scores.getExplainedIntensityRatio());
        row.add(scores.getMedianMassDeviation().getPpm());
        row.add(scores.getMedianAbsoluteMassDeviation().getPpm());

        row.add(tree.getMassErrorTo(tree.getRoot(), f.getAverageMass()).getPpm());

        row.add(tree.getAnnotation(LipidSpecies.class).map(LipidSpecies::toString).orElse(""));

        row.add(f.getAverageMass());
        row.add(Optional.ofNullable(f.getRetentionTime()).map(rt -> Math.round(rt.getMiddleTime())).orElse(null));
        row.add(Optional.ofNullable(f.getRetentionTime()).map(rt -> rt.getMiddleTime() / 60d).orElse(null));
        row.add(String.valueOf(fc.getFormulaId()));
        row.add(String.valueOf(fc.getAlignedFeatureId()));
        row.add(getMappingIdOrFallback(f));
        row.add(f.getDataQuality());

        writer.writeRow(row);
    }
}
