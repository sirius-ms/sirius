/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.summaries;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

class NoSqlFormulaSummaryWriter implements AutoCloseable {
    final static String DOUBLE_FORMAT = "%.3f";
    final static String LONG_FORMAT = "%d";
    final static String HEADER = "formulaRank\t" +
            "molecularFormula\t" +
            "adduct\t" +
            "precursorFormula\t" +
            "neutralizedPrecursorFormula(treeFormula)\t" +
            "SiriusScore\t" +
            "TreeScore\t" +
            "IsotopeScore\t" +
            "numExplainedPeaks\t" +
            "explainedIntensity\t" +

            "medianMassErrorFragmentPeaks(ppm)\t" +
            "medianAbsoluteMassErrorFragmentPeaks(ppm)\t" +

            "massErrorPrecursor(ppm)\t" +

            "lipidClass\t" +
            "ionMass\t" +
            "retentionTimeInSeconds\t" +
            "formulaId\t" +
            "alignedFeatureId\t" +
            "providedFeatureId";

    private final BufferedWriter w;





    NoSqlFormulaSummaryWriter(BufferedWriter writer) {
        this.w = writer;
    }

    private NoSqlFormulaSummaryWriter(Writer w) {
        this.w = new BufferedWriter(w);
    }

    public void writeHeader() throws IOException {
        w.write(HEADER);
        w.newLine();
    }

    public void writeFormulaCandidate(AlignedFeatures f, FormulaCandidate fc, FTree tree) throws IOException {
        FTreeMetricsHelper scores = new FTreeMetricsHelper(tree);
        w.write(String.valueOf(fc.getFormulaRank()));
        writeSep();
        w.write(fc.getMolecularFormula().toString());
        writeSep();
        w.write(fc.getAdduct().toString());
        writeSep();
        w.write(fc.getAdduct().neutralMoleculeToPrecursorIon(fc.getMolecularFormula()).toString());
        writeSep();
        w.write(fc.getAdduct().neutralMoleculeToMeasuredNeutralMolecule(fc.getMolecularFormula()).toString());
        writeSep();
        w.write(String.format(DOUBLE_FORMAT, fc.getSiriusScore()));
        writeSep();
        w.write(String.format(DOUBLE_FORMAT, fc.getTreeScore()));
        writeSep();
        w.write(String.format(DOUBLE_FORMAT, fc.getIsotopeScore()));
        writeSep();
        w.write(String.format(LONG_FORMAT, scores.getNumOfExplainedPeaks()));
        writeSep();
        w.write(String.format(DOUBLE_FORMAT, scores.getExplainedIntensityRatio()));
        writeSep();
        w.write(String.format(DOUBLE_FORMAT, scores.getMedianMassDeviation().getPpm()));
        writeSep();
        w.write(String.format(DOUBLE_FORMAT, scores.getMedianAbsoluteMassDeviation().getPpm()));
        writeSep();

        w.write(String.format(DOUBLE_FORMAT, tree.getMassErrorTo(tree.getRoot(), f.getAverageMass()).getPpm()));
        writeSep();

        w.write(tree.getAnnotation(LipidSpecies.class).map(LipidSpecies::toString).orElse(""));
        writeSep();

        w.write(String.format(DOUBLE_FORMAT, f.getAverageMass()));
        writeSep();
        w.write(String.format("%.0f", f.getRetentionTime().getMiddleTime()));
        writeSep();
        w.write(String.format(LONG_FORMAT, fc.getFormulaId()));
        writeSep();
        w.write(String.format(LONG_FORMAT, fc.getAlignedFeatureId()));
        writeSep();
        w.write(Objects.requireNonNullElse(f.getExternalFeatureId(), ""));
        w.newLine();
    }

    private void writeSep() throws IOException {
        w.write('\t');
    }

    @Override
    public void close() throws Exception {
        w.close();
    }
}
