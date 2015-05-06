package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.MultipleTreeComputation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TreeIterator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.TreeSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.DecompositionList;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.sirius.elementpred.ElementPrediction;

import java.io.IOException;
import java.util.*;

public class Sirius {

    public final static String VERSION_STRING = "Sirius 3.0.0";

    public final static String CITATION = "Sebastian Böcker and Florian Rasche\n" +
            "Towards de novo identification of metabolites by analyzing tandem mass spectra.\n" +
            "Bioinformatics, 24:I49-I55, 2008. Proc. of European Conference on Computational Biology (ECCB 2008).\n" +
            "\n" +
            "Kerstin Scheubert, Franziska Hufsky, Florian Rasche and Sebastian Böcker\n" +
            "Computing fragmentation trees from metabolite multiple mass spectrometry data.\n" +
            "In Proc. of Research in Computational Molecular Biology (RECOMB 2011), volume 6577 of Lect Notes Comput Sci, pages 377-391. Springer, Berlin, 2011.\n\n" +
            "Kai Dührkop and Sebastian Böcker\n" +
            "Fragmentation trees reloaded.\n" +
            "In Proc. of Research in Computational Molecular Biology (RECOMB 2015), volume 9029 of Lect Notes Comput Sci, pages 65-79. 2015.";

    private static final double MAX_TREESIZE_INCREASE = 3d;
    private static final double TREE_SIZE_INCREASE = 1d;
    private static final int MIN_NUMBER_OF_EXPLAINED_PEAKS = 15;
    private static final double MIN_EXPLAINED_INTENSITY = 0.8d;

    protected Profile profile;
    protected ElementPrediction elementPrediction;
    protected Progress progress;


    public final static String ISOTOPE_SCORE = "isotope";

    public Sirius(String profileName) throws IOException {
        profile = new Profile(profileName);
        loadMeasurementProfile();
    }

    public Sirius() {
        try {
            profile = new Profile("default");
            loadMeasurementProfile();
            this.progress = new Progress.Quiet();
        } catch (IOException e) { // should be in classpath
            throw new RuntimeException(e);
        }
    }

    public Progress getProgress() {
        return progress;
    }

    public void setProgress(Progress progress) {
        this.progress = progress;
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
        this.elementPrediction = new ElementPrediction(profile.isotopePatternAnalysis);
    }

    /**
     *
     * Identify the molecular formula of the measured compound using the provided MS and MSMS data
     *
     * @param uexperiment input data
     *
     * @return a list of identified molecular formulas together with their tree
     */
    public List<IdentificationResult> identify(Ms2Experiment uexperiment) {
        return identify(uexperiment, 5, true, IsotopePatternHandling.score, Collections.<MolecularFormula>emptySet());
    }

    /**
     *
     * Identify the molecular formula of the measured compound using the provided MS and MSMS data
     *
     * @param uexperiment input data
     * @param numberOfCandidates number of candidates to output
     * @param recalibrating true if spectra should be recalibrated during tree computation
     * @param deisotope set this to 'omit' to ignore isotope pattern, 'filter' to use it for selecting molecular formula candidates or 'score' to rerank the candidates according to their isotope pattern
     * @param whiteList restrict the analysis to this subset of molecular formulas. If this set is empty, consider all possible molecular formulas
     * @return a list of identified molecular formulas together with their tree
     */
    public List<IdentificationResult> identify(Ms2Experiment uexperiment, int numberOfCandidates, boolean recalibrating, IsotopePatternHandling deisotope, Set<MolecularFormula> whiteList) {
        MutableMs2Experiment experiment = new MutableMs2Experiment(extendConstraints(profile.fragmentationPatternAnalysis.validate(uexperiment), progress));
        // first check if MS data is present;
        final List<IsotopePattern> candidates = lookAtMs1(experiment, deisotope!=IsotopePatternHandling.omit);
        int maxNumberOfFormulas = 0;
        final HashMap<MolecularFormula, Double> isoFormulas = new HashMap<MolecularFormula, Double>();
        final double optIsoScore;
        if (candidates.size() > 0) {
            Collections.sort(candidates, new Comparator<IsotopePattern>() {
                @Override
                public int compare(IsotopePattern o1, IsotopePattern o2) {
                    return Double.compare(o2.getBestScore(),o1.getBestScore());
                }
            });
            final IsotopePattern pattern = candidates.get(0);
            optIsoScore = filterCandidateList(pattern, isoFormulas);
        } else optIsoScore = 0d;

        ProcessedInput pinput = profile.fragmentationPatternAnalysis.preprocessing(experiment);

        if (isoFormulas.size() > 0 && optIsoScore>10) {
            maxNumberOfFormulas = isoFormulas.size();
        } else {
            maxNumberOfFormulas = pinput.getPeakAnnotationOrThrow(DecompositionList.class).get(pinput.getParentPeak()).getDecompositions().size();
        }

        final int outputSize = Math.min(maxNumberOfFormulas, numberOfCandidates);
        final int computeNTrees = Math.max(5, outputSize);

        final TreeSizeScorer treeSizeScorer = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, profile.fragmentationPatternAnalysis.getFragmentPeakScorers());
        final double originalTreeSize = (treeSizeScorer!=null ? treeSizeScorer.getTreeSizeScore() : 0d);
        double modifiedTreeSizeScore = originalTreeSize;
        final double MAX_TREESIZE_SCORE = originalTreeSize+MAX_TREESIZE_INCREASE;

        final ArrayList<FTree> computedTrees = new ArrayList<FTree>();

        try {
            while (true) {
                MultipleTreeComputation trees = profile.fragmentationPatternAnalysis.computeTrees(pinput);
                trees = trees.inParallel(3);
                if (isoFormulas.size() > 0 && optIsoScore>10) {
                    trees = trees.onlyWith(isoFormulas.keySet());
                }
                if (whiteList.size() > 0) {
                    trees = trees.onlyWith(whiteList);
                }
                trees = trees.computeMaximal(computeNTrees).withoutRecalibration();

                final TreeSet<FTree> treeSet = new TreeSet<FTree>(TREE_SCORE_COMPARATOR);

                final TreeIterator iter = trees.iterator(true);
                progress.init(maxNumberOfFormulas);
                int counter=0;
                while (iter.hasNext()) {
                    final FTree tree = iter.next();
                    if (deisotope == IsotopePatternHandling.score) addIsoScore(isoFormulas, tree);

                    if (tree != null) {
                        treeSet.add(tree);
                        if (treeSet.size() > numberOfCandidates) treeSet.pollFirst();
                    }
                    if (iter.lastGraph()!=null)
                        progress.update(++counter, maxNumberOfFormulas, iter.lastGraph().getRoot().getChildren(0).getFormula().toString());
                }
                progress.finished();

                // check if at least one of the best N trees satisfies the tree-rejection-condition
                boolean satisfied = treeSizeScorer == null || modifiedTreeSizeScore >= MAX_TREESIZE_SCORE;
                if (!satisfied) {
                    final Iterator<FTree> treeIterator = treeSet.descendingIterator();
                    for (int k=0; k < computeNTrees; ++k) {
                        if (treeIterator.hasNext()) {
                            final FTree tree = treeIterator.next();
                            final double intensity = profile.fragmentationPatternAnalysis.getIntensityRatioOfExplainedPeaks(tree);
                            if (tree.numberOfVertices()>=MIN_NUMBER_OF_EXPLAINED_PEAKS || intensity >= MIN_EXPLAINED_INTENSITY) {
                                satisfied=true; break;
                            }
                        } else break;
                    }
                }
                if (satisfied) {
                    computedTrees.addAll(treeSet.descendingSet());
                    break;
                } else {
                    progress.info("Not enough peaks were explained. Repeat computation with less restricted constraints.");
                    modifiedTreeSizeScore += TREE_SIZE_INCREASE;
                    treeSizeScorer.setTreeSizeScore(modifiedTreeSizeScore);
                    computedTrees.clear();
                    // TODO!!!! find a smarter way to do this -_-
                    pinput = profile.fragmentationPatternAnalysis.preprocessing(experiment);
                }
            }

            if (recalibrating) {
                // now recalibrate the trees and recompute them another time...
                progress.info("recalibrate trees");
                progress.init(computedTrees.size());
                for (int k=0; k < computedTrees.size(); ++k) {
                    final FTree recalibratedTree = profile.fragmentationPatternAnalysis.recalibrate(computedTrees.get(k), true);
                    if (deisotope== IsotopePatternHandling.score) addIsoScore(isoFormulas, recalibratedTree);
                    computedTrees.set(k, recalibratedTree);
                    progress.update(k+1, computedTrees.size(), "recalibrate " + recalibratedTree.getRoot().getFormula().toString());
                }
                progress.finished();
            }

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

    /**
     * check MS spectrum. If an isotope pattern is found, check it's monoisotopic mass and update the ionmass field
     * if this field is null yet
     * If deisotope is set, start isotope pattern analysis
     * @return
     */
    private List<IsotopePattern> lookAtMs1(MutableMs2Experiment experiment, boolean deisotope) {
        if (experiment.getIonMass()==0) {
            if (experiment.getMs1Spectra().size()==0)
                throw new RuntimeException("Please provide the parentmass of the measured compound");
            final List<IsotopePattern> candidates = profile.isotopePatternAnalysis.deisotope(experiment);
            if (candidates.size() > 1) {
                throw new RuntimeException("Please provide the parentmass of the measured compound");
            }
            experiment.setIonMass(candidates.get(0).getMonoisotopicMass());
        }
        return deisotope ? profile.isotopePatternAnalysis.deisotope(experiment, experiment.getIonMass(), false) : Collections.<IsotopePattern>emptyList();
    }

    private Ms2Experiment extendConstraints(Ms2Experiment experiment, Progress progress) {
        final FormulaConstraints constraints;
        if (experiment.getMeasurementProfile()!=null && experiment.getMeasurementProfile().getFormulaConstraints()!=null) {
            constraints = experiment.getMeasurementProfile().getFormulaConstraints();
        } else constraints = profile.fragmentationPatternAnalysis.getDefaultProfile().getFormulaConstraints();
        final MeasurementProfile mpr = experiment.getMeasurementProfile()==null ? profile.fragmentationPatternAnalysis.getDefaultProfile() : MutableMeasurementProfile.merge(profile.fragmentationPatternAnalysis.getDefaultProfile(), experiment.getMeasurementProfile());
        final FormulaConstraints newC = elementPrediction.extendConstraints(constraints, experiment, mpr);
        if (newC != constraints) {
            progress.info("Extend alphabet to " + newC.getChemicalAlphabet().toString());
            final MutableMs2Experiment newExp = new MutableMs2Experiment(experiment);
            final MutableMeasurementProfile newProf = experiment.getMeasurementProfile()!= null ? new MutableMeasurementProfile(experiment.getMeasurementProfile()) : new MutableMeasurementProfile();
            newProf.setFormulaConstraints(newC);
            newExp.setMeasurementProfile(newProf);
            return newExp;
        } else return experiment;
    }

    private void addIsoScore(HashMap<MolecularFormula, Double> isoFormulas, FTree tree) {
        final TreeScoring sc = tree.getAnnotationOrThrow(TreeScoring.class);
        if (isoFormulas.get(tree.getRoot().getFormula())!=null) {
            sc.addAdditionalScore(ISOTOPE_SCORE, isoFormulas.get(tree.getRoot().getFormula()));
        }
    }

    public IdentificationResult compute(Ms2Experiment experiment, MolecularFormula formula) {
        return compute(experiment, formula, true);
    }

    public IdentificationResult compute(Ms2Experiment experiment, MolecularFormula formula, boolean recalibrating) {
        ProcessedInput pinput = profile.fragmentationPatternAnalysis.preprocessing(experiment);
        final TreeSizeScorer treeSizeScorer = FragmentationPatternAnalysis.getByClassName(TreeSizeScorer.class, profile.fragmentationPatternAnalysis.getFragmentPeakScorers());
        final double originalTreeSize = (treeSizeScorer!=null ? treeSizeScorer.getTreeSizeScore() : 0d);
        double modifiedTreeSizeScore = originalTreeSize;
        final double MAX_TREESIZE_SCORE = originalTreeSize+MAX_TREESIZE_INCREASE;
        FTree tree = null;
        try {
            while (true) {
                tree = profile.fragmentationPatternAnalysis.computeTrees(pinput).withRecalibration(recalibrating).onlyWith(Arrays.asList(formula)).optimalTree();
                if (tree==null) return new IdentificationResult(null, 0);
                final double intensity = profile.fragmentationPatternAnalysis.getIntensityRatioOfExplainablePeaks(tree);
                if (treeSizeScorer == null || modifiedTreeSizeScore >= MAX_TREESIZE_SCORE || tree.numberOfVertices()>=MIN_NUMBER_OF_EXPLAINED_PEAKS || intensity >= MIN_EXPLAINED_INTENSITY) {
                    break;
                } else {
                    modifiedTreeSizeScore += TREE_SIZE_INCREASE;
                    treeSizeScorer.setTreeSizeScore(modifiedTreeSizeScore);
                    // TODO!!!! find a smarter way to do this -_-
                    pinput = profile.fragmentationPatternAnalysis.preprocessing(experiment);
                }
            }
            profile.fragmentationPatternAnalysis.recalculateScores(tree);
        } finally {
            treeSizeScorer.setTreeSizeScore(originalTreeSize);
        }
        return new IdentificationResult(tree, 0);
    }

    private double filterCandidateList(IsotopePattern candidate, HashMap<MolecularFormula, Double> formulas) {
        if (candidate.getCandidates().size()==0) return 0d;
        if (candidate.getBestScore() <= 0) return 0d;
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
        return optscore;
    }

    private static Comparator<FTree> TREE_SCORE_COMPARATOR = new Comparator<FTree>() {
        @Override
        public int compare(FTree o1, FTree o2) {
            return Double.compare(o1.getAnnotationOrThrow(TreeScoring.class).getOverallScore(), o2.getAnnotationOrThrow(TreeScoring.class).getOverallScore());
        }
    };
}
