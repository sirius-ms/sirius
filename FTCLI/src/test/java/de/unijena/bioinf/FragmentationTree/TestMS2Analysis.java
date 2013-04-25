package de.unijena.bioinf.FragmentationTree;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.Hydrogen2CarbonScorer;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.ChemicalPriorScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.PeakIsNoiseScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationGraph;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProfileImpl;
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
            final ProfileImpl profile = new ProfileImpl();
            profile.setExpectedFragmentMassDeviation(new Deviation(20, 2e-3));
            profile.setExpectedIonMassDeviation(new Deviation(20, 2e-3));
            profile.setFormulaConstraints(new FormulaConstraints(new ChemicalAlphabet()));
            experiment.setMeasurementProfile(profile);
            final StringWriter writer = new StringWriter();
            final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();
            analysis.getFragmentPeakScorers().clear();
            analysis.getFragmentPeakScorers().add(new PeakIsNoiseScorer(10000));
            analysis.getDecompositionScorers().add(new ChemicalPriorScorer(new Hydrogen2CarbonScorer()));
            final ProcessedInput processed = analysis.preprocessing(experiment);
            final FragmentationGraph graph = analysis.buildGraph(processed, new ScoredMolecularFormula(experiment.getMolecularFormula().add(MolecularFormula.parse("H")), 0d));
            final FragmentationTree tree = analysis.computeTree(graph);
            System.out.println(tree.getScore());
            final TreeAnnotation annotation = new TreeAnnotation(tree, analysis);
            final FTDotWriter dotWriter = new FTDotWriter();
            dotWriter.writeTree(writer, tree, annotation.getVertexAnnotations(), annotation.getEdgeAnnotations());
            System.out.println(writer.toString());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
