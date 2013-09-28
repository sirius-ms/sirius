package de.unijena.bioinf.FragmentationTree;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TreeIterator;
import de.unijena.bioinf.FragmentationTreeConstruction.inspection.TreeAnnotation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.ms.JenaMsParser;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExampleMain {

    public static void computeTrees(String fileName) throws IOException {

        // load profile
        final Profile profile = new Profile("default");
        // hint: do this only once for all files ;)

        // load ms file
        final Ms2Experiment experiment = new GenericParser<Ms2Experiment>(new JenaMsParser()).parseFile(new File(fileName));

        // analyze ms/ms with SIRIUS
        final ProcessedInput preprocessing = profile.fragmentationPatternAnalysis.preprocessing(experiment);
        // compute all trees
        final List<FragmentationTree> trees = profile.fragmentationPatternAnalysis.computeTrees(preprocessing).list();
        // compute trees one by one (e.g. in a loop)
        final TreeIterator iterator = profile.fragmentationPatternAnalysis.computeTrees(preprocessing).iterator();
        // iterator behaves like a normal iterator
        // compute only optimal tree
        final FragmentationTree optimal = profile.fragmentationPatternAnalysis.computeTrees(preprocessing).optimalTree();
        // compute tree for a certain molecular formula
        final FragmentationTree special = profile.fragmentationPatternAnalysis.computeTrees(preprocessing).onlyWith(Arrays.asList(MolecularFormula.parse("C6H12O6"))).optimalTree();

        // analyze ms1 with SIRIUS (IN DEVELOPMENT! API MAY CHANGE!)
        final List<IsotopePattern> patterns = profile.isotopePatternAnalysis.deisotope(experiment);

        // list contains a IsotopePattern for each compound in the MS1 spectrum (may not work correct currently)
        final IsotopePattern onePattern = patterns.get(0);
        // get candidate list
        final List<ScoredMolecularFormula> candidates = onePattern.getCandidates();
        // get best hit
        final ScoredMolecularFormula best = candidates.get(0);
        // compute tree for best hit
        final FragmentationTree bestHitTree = profile.fragmentationPatternAnalysis.computeTrees(preprocessing).onlyWith(Arrays.asList(best.getFormula())).optimalTree();

        // annotate tree
        final TreeAnnotation annotation = new TreeAnnotation(bestHitTree, profile.fragmentationPatternAnalysis, preprocessing);
        // add additional information to annotation
        annotation.getAdditionalProperties().put(bestHitTree.getRoot(), Arrays.asList("IsotopeScore: " + best.getScore()));

    }

}
