package de.unijena.bioinf.sirius;

import com.google.common.collect.Iterators;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.MultipleTreeComputation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TreeIterator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.TreeSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.DecompositionList;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.mgf.MgfParser;
import de.unijena.bioinf.babelms.ms.CsvParser;
import de.unijena.bioinf.sirius.cli.Instance;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Sirius {

    private static final double MAX_TREESIZE_INCREASE = 2d;
    private static final double TREE_SIZE_INCREASE = 1d;
    private static final int MIN_NUMBER_OF_EXPLAINED_PEAKS = 15;
    private static final double MIN_EXPLAINED_INTENSITY = 0.85d;

    protected Profile profile;

    public Sirius(String profileName) throws IOException {
        profile = new Profile(profileName);
        loadMeasurementProfile();
    }

    public Sirius() {
        try {
            profile = new Profile("default");
            loadMeasurementProfile();
        } catch (IOException e) { // should be in classpath
            throw new RuntimeException(e);
        }
    }

    public FragmentationPatternAnalysis getMs2Analyzer() {
        return profile.fragmentationPatternAnalysis;
    }

    public IsotopePatternAnalysis getMs1Analyzer() {
        return profile.isotopePatternAnalysis;
    }

    private void loadMeasurementProfile() {
        // make mutable
        profile.fragmentationPatternAnalysis.setDefaultProfile(new MutableMeasurementProfile(profile.fragmentationPatternAnalysis.getDefaultProfile()));
        profile.isotopePatternAnalysis.setDefaultProfile(new MutableMeasurementProfile(profile.isotopePatternAnalysis.getDefaultProfile()));
    }

    public List<IdentificationResult> identify(Ms2Experiment experiment, IdentifyOptions opts, Progress progress) {
        // first check if MS data is present
        final List<IsotopePattern> candidates = profile.isotopePatternAnalysis.deisotope(experiment, experiment.getIonMass(), false);
        int maxNumberOfFormulas = 0;
        final HashMap<MolecularFormula, Double> isoFormulas = new HashMap<MolecularFormula, Double>();
        if (candidates.size() > 0) {
            final IsotopePattern pattern = candidates.get(0);
            filterCandidateList(pattern, isoFormulas);
        }

        final ProcessedInput pinput = profile.fragmentationPatternAnalysis.preprocessing(experiment);
        MultipleTreeComputation trees = profile.fragmentationPatternAnalysis.computeTrees(pinput);

        if (isoFormulas.size() > 0) {
            trees = trees.onlyWith(isoFormulas.keySet());
            maxNumberOfFormulas = isoFormulas.size();
        } else {
            maxNumberOfFormulas = pinput.getPeakAnnotationOrThrow(DecompositionList.class).get(pinput.getParentPeak()).getDecompositions().size();
        }

        final int outputSize = Math.min(maxNumberOfFormulas, opts.getNumberOfCandidates());
        final int computeNTrees = Math.max(5, outputSize);
        trees = trees.computeMaximal(computeNTrees).withoutRecalibration();

        final TreeSizeScorer treeSizeScorer = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, profile.fragmentationPatternAnalysis.getFragmentPeakScorers());
        final double originalTreeSize = (treeSizeScorer!=null ? treeSizeScorer.getTreeSizeScore() : 0d);
        double modifiedTreeSizeScore = originalTreeSize;
        final double MAX_TREESIZE_SCORE = originalTreeSize+MAX_TREESIZE_INCREASE;

        final ArrayList<FTree> computedTrees = new ArrayList<FTree>();

        try {
            while (true) {
                final TreeSet<FTree> treeSet = new TreeSet<FTree>(TREE_SCORE_COMPARATOR);

                final TreeIterator iter = trees.iterator(true);
                progress.init(maxNumberOfFormulas);
                int counter=0;
                while (iter.hasNext()) {
                    final FTree tree = iter.next();
                    if (tree != null) {
                        treeSet.add(tree);
                        if (treeSet.size() > opts.getNumberOfCandidates()) treeSet.pollFirst();
                    }
                    if (iter.lastGraph()!=null)
                        progress.update(++counter, maxNumberOfFormulas, iter.lastGraph().getRoot().getChildren(0).getFormula().toString());
                }
                progress.finished();

                // check if at least one of the best N trees satisfies the tree-rejection-condition
                if (treeSizeScorer == null || modifiedTreeSizeScore >= MAX_TREESIZE_SCORE) {
                    computedTrees.addAll(treeSet.descendingSet());
                }
                final Iterator<FTree> treeIterator = treeSet.descendingIterator();
                boolean satisfied = false;
                for (int k=0; k < computeNTrees; ++k) {
                    if (treeIterator.hasNext()) {
                        final FTree tree = treeIterator.next();
                        final double intensity = profile.fragmentationPatternAnalysis.getIntensityRatioOfExplainedPeaks(tree);
                        if (tree.numberOfVertices()>=MIN_NUMBER_OF_EXPLAINED_PEAKS || intensity >= MIN_EXPLAINED_INTENSITY) {
                            satisfied=true; break;
                        }
                    } else break;
                }
                if (satisfied) {
                    computedTrees.addAll(treeSet.descendingSet());
                    break;
                } else {
                    progress.info("Not enough peaks were explained. Repeat computation with less restricted constraints.");
                    modifiedTreeSizeScore += TREE_SIZE_INCREASE;
                    treeSizeScorer.setTreeSizeScore(modifiedTreeSizeScore);
                }
            }

            // now recalibrate the trees and recompute them another time...
            progress.info("recalibrate trees");
            progress.init(computedTrees.size());
            for (int k=0; k < computedTrees.size(); ++k) {
                final FTree recalibratedTree = profile.fragmentationPatternAnalysis.recalibrate(computedTrees.get(k), true);
                computedTrees.set(k, recalibratedTree);
                progress.update(k+1, computedTrees.size(), "recalibrate " + recalibratedTree.getRoot().getFormula().toString());
            }
            progress.finished();
            Collections.sort(computedTrees, Collections.reverseOrder(TREE_SCORE_COMPARATOR));


            final ArrayList<IdentificationResult> list = new ArrayList<IdentificationResult>(outputSize);
            for (int k=0; k < Math.min(outputSize, computedTrees.size()); ++k) {
                final FTree tree = computedTrees.get(k);
                profile.fragmentationPatternAnalysis.recalculateScores(tree);
                list.add(new IdentificationResult(tree, k+1));
            }
            return list;
        } finally {
            treeSizeScorer.setTreeSizeScore(originalTreeSize);
        }
    }

    public IdentificationResult compute(Ms2Experiment experiment, MolecularFormula formula, TreeOptions opts) {
        final ProcessedInput pinput = profile.fragmentationPatternAnalysis.preprocessing(experiment);
        final TreeSizeScorer treeSizeScorer = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, profile.fragmentationPatternAnalysis.getFragmentPeakScorers());
        final double originalTreeSize = (treeSizeScorer!=null ? treeSizeScorer.getTreeSizeScore() : 0d);
        double modifiedTreeSizeScore = originalTreeSize;
        final double MAX_TREESIZE_SCORE = originalTreeSize+2d;
        FTree tree = null;
        try {
            while (true) {
                tree = profile.fragmentationPatternAnalysis.computeTrees(pinput).withRecalibration().onlyWith(Arrays.asList(formula)).optimalTree();
                final double intensity = profile.fragmentationPatternAnalysis.getIntensityRatioOfExplainedPeaks(tree);
                if (treeSizeScorer == null || modifiedTreeSizeScore >= MAX_TREESIZE_SCORE || tree.numberOfVertices()>=MIN_NUMBER_OF_EXPLAINED_PEAKS || intensity >= MIN_EXPLAINED_INTENSITY) {
                    break;
                } else {
                    modifiedTreeSizeScore += TREE_SIZE_INCREASE;
                    treeSizeScorer.setTreeSizeScore(modifiedTreeSizeScore);
                }
            }
            profile.fragmentationPatternAnalysis.recalculateScores(tree);
        } finally {
            treeSizeScorer.setTreeSizeScore(originalTreeSize);
        }
        return new IdentificationResult(tree, 0);
    }

    public Iterator<Instance> readAllInstances(List<File> files) {
        return null;
    }

    public Instance readFrom(File file) throws IOException {
        final GenericParser<Ms2Experiment> parser = new MsExperimentParser().getParser(file);
        if (parser==null) {
            return readFrom(null, Arrays.asList(file));
        } else return new Instance(parser.parseFromFile(file).get(0), file);
    }

    public Instance readFrom(File ms1, List<File> ms2) throws IOException {
        // expect ms1 and ms2 to be spectral files
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        final ArrayList<SimpleSpectrum> ms1Spectra = new ArrayList<SimpleSpectrum>();
        final ArrayList<Ms2Spectrum<Peak>> ms2Spectra = new ArrayList<Ms2Spectrum<Peak>>();
        exp.setMs1Spectra(ms1Spectra);
        exp.setMs2Spectra(ms2Spectra);

        final Iterator<Ms2Spectrum<Peak>> ms1i;
        if (ms1 != null && ms1.getName().endsWith(".mgf")) {
            ms1i = new MgfParser().parseSpectra(ms1);
        } else if (ms1 != null) {
            ms1i = new CsvParser().parseSpectra(ms1);
        } else ms1i = Iterators.emptyIterator();
        Iterator<Ms2Spectrum<Peak>> ms2i = Iterators.emptyIterator();
        for (File g : ms2) {
            if (g.getName().endsWith(".mgf")) {
                ms2i = Iterators.concat(ms2i, new MgfParser().parseSpectra(ms1));
            } else {
                ms2i = Iterators.concat(ms2i, new CsvParser().parseSpectra(ms1));
            }
        }
        while (ms1i.hasNext())
            ms1Spectra.add(new SimpleSpectrum(ms1i.next()));
        while (ms2i.hasNext())
            ms2Spectra.add(ms2i.next());
        if (ms2Spectra.size() > 0) {
            exp.setIonization(ms2Spectra.get(0).getIonization());
            for (int k=1; k < ms2Spectra.size(); ++k) {
                if (!ms2Spectra.get(k).getIonization().equals(exp.getIonization())) {
                    throw new RuntimeException("SIRIUS currently does not support different MS/MS spectra with different ionization modes");
                }
            }
            exp.setIonMass(ms2Spectra.get(0).getPrecursorMz());
            for (int k=1; k < ms2Spectra.size(); ++k) {
                if (Math.abs(ms2Spectra.get(k).getPrecursorMz() - exp.getIonMass()) > 1e-2) {
                    throw new RuntimeException("MS/MS spectra of the same compound have different precursor mass");
                }
            }
            if (exp.getIonMass()==0) {
                throw new RuntimeException("There is no precursor mass given in the input file");
            }
        }
        return new Instance(exp, ms2.get(0));
    }

    private void filterCandidateList(IsotopePattern candidate, HashMap<MolecularFormula, Double> formulas) {
        if (candidate.getCandidates().size()==0) return;
        if (candidate.getBestScore() <= 0) return;
        final double optscore = candidate.getBestScore();
        final ArrayList<ScoredMolecularFormula> xs = new ArrayList<ScoredMolecularFormula>(candidate.getCandidates());
        Collections.sort(xs, Collections.reverseOrder());
        int n = 1;
        for (; n < xs.size(); ++n) {
            final double score = xs.get(n).getScore();
            final double prev = xs.get(n-1).getScore();
            if (score <= 0 || score/optscore < 0.666 || score/prev < 0.5) break;
        }
        for (int i=0; i < n; ++i) formulas.put(xs.get(i).getFormula(), xs.get(i).getScore());
    }

    private static Comparator<FTree> TREE_SCORE_COMPARATOR = new Comparator<FTree>() {
        @Override
        public int compare(FTree o1, FTree o2) {
            return Double.compare(o1.getAnnotationOrThrow(TreeScoring.class).getOverallScore(), o2.getAnnotationOrThrow(TreeScoring.class).getOverallScore());
        }
    };
}
