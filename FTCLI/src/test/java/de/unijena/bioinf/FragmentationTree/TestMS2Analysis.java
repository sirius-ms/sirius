/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTree;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.Hydrogen2CarbonScorer;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.ChemicalPriorScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.PeakIsNoiseScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.ms.JenaMsExperiment;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

public class TestMS2Analysis {

    @Test
    public void testPipeline() {
        final FragmentationPatternAnalysis patternAnalysis = new FragmentationPatternAnalysis();
        try {
            final JenaMsExperiment experiment = new GenericParser<Ms2Experiment>(new JenaMsParser()).parseFile(new File(this.getClass().getClassLoader().getResource("Apomorphine.ms").getFile()));
            final MutableMeasurementProfile profile = new MutableMeasurementProfile();
            profile.setAllowedMassDeviation(new Deviation(20, 2e-3));
            profile.setStandardMs2MassDeviation(new Deviation(20, 2e-3));
            profile.setMedianNoiseIntensity(0.01);
            profile.setFormulaConstraints(new FormulaConstraints(new ChemicalAlphabet()));
            experiment.setMeasurementProfile(profile);
            final StringWriter writer = new StringWriter();

            final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();
            analysis.setDefaultProfile(profile);


            analysis.getFragmentPeakScorers().clear();
            analysis.getFragmentPeakScorers().add(new PeakIsNoiseScorer());
            analysis.getDecompositionScorers().add(new ChemicalPriorScorer(new Hydrogen2CarbonScorer(), 0d));
            final ProcessedInput processed = analysis.preprocessing(experiment);
            final FGraph graph = analysis.buildGraph(processed, new ScoredMolecularFormula(experiment.getMolecularFormula(), 0d));
            final FTree tree = analysis.computeTree(graph);
            System.out.println(tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore());
            final TreeAnnotation annotation = new TreeAnnotation(tree, analysis);
            final FTDotWriter dotWriter = new FTDotWriter();
            dotWriter.writeTree(writer, tree, annotation.getAdditionalProperties(), annotation.getVertexAnnotations(), annotation.getEdgeAnnotations());
            System.out.println(writer.toString());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
