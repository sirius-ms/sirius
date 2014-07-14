package de.unijena.bioinf.FragmentationTreeConstruction.computation;


import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.Hetero2CarbonScorer;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.ExponentialDistribution;
import de.unijena.bioinf.ChemistryBase.math.LogNormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.GraphBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.GraphReduction;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.SubFormulaGraphBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.InputValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.MissingValueValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.Warning;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.HighIntensityMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.Merger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.PeakMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.RecalibrationMethod;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.DPTreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.MassDecomposer.Chemistry.DecomposerCache;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

import java.util.*;

public class FragmentationPatternAnalysis implements Parameterized, Cloneable {

    public static final String ANALYZER_NAME = "FragmentationPatternAnalysis";

    private List<InputValidator> inputValidators;
    private Warning validatorWarning;
    private boolean repairInput;
    private NormalizationType normalizationType;
    private PeakMerger peakMerger;
    private DecomposerCache decomposers;
    private List<DecompositionScorer<?>> decompositionScorers;
    private List<DecompositionScorer<?>> rootScorers;
    private List<LossScorer> lossScorers;
    private List<PeakPairScorer> peakPairScorers;
    private List<PeakScorer> fragmentPeakScorers;
    private GraphBuilder graphBuilder;
    private List<Preprocessor> preprocessors;
    private List<PostProcessor> postProcessors;
    private TreeBuilder treeBuilder;
    private MutableMeasurementProfile defaultProfile;
    private RecalibrationMethod recalibrationMethod;
    private GraphReduction reduction;

    /**
     *
     */
    public FragmentationPatternAnalysis() {
        this.decomposers = new DecomposerCache();
        setInitial();
    }

    public static <G, D, L> FragmentationPatternAnalysis loadFromProfile(DataDocument<G, D, L> document, G value) {
        final ParameterHelper helper = ParameterHelper.getParameterHelper();
        final D dict = document.getDictionary(value);
        if (!document.hasKeyInDictionary(dict, "FragmentationPatternAnalysis"))
            throw new IllegalArgumentException("No field 'FragmentationPatternAnalysis' in profile");
        final FragmentationPatternAnalysis analyzer = (FragmentationPatternAnalysis) helper.unwrap(document,
                document.getFromDictionary(dict, "FragmentationPatternAnalysis"));
        if (document.hasKeyInDictionary(dict, "profile")) {
            final MeasurementProfile prof = ((MeasurementProfile) helper.unwrap(document, document.getFromDictionary(dict, "profile")));
            if (analyzer.defaultProfile == null) analyzer.defaultProfile = new MutableMeasurementProfile(prof);
            else
                analyzer.defaultProfile = new MutableMeasurementProfile(MutableMeasurementProfile.merge(prof, analyzer.defaultProfile));
        }
        return analyzer;
    }

    /**
     * Construct a new FragmentationPatternAnaylsis with default scorers
     */
    public static FragmentationPatternAnalysis oldSiriusAnalyzer() {
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();

        // peak pair scorers
        final RelativeLossSizeScorer lossSize = new RelativeLossSizeScorer();
        final List<PeakPairScorer> peakPairScorers = new ArrayList<PeakPairScorer>();
        peakPairScorers.add(new CollisionEnergyEdgeScorer(0.1, 0.8));
        peakPairScorers.add(lossSize);

        // loss scorers
        final List<LossScorer> lossScorers = new ArrayList<LossScorer>();
        lossScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet());
        lossScorers.add(new DBELossScorer());
        lossScorers.add(new PureCarbonNitrogenLossScorer());
        //lossScorers.add(new ChemicalPriorEdgeScorer(new Hetero2CarbonScorer(new NormalDistribution(0.5886335d, 0.5550574d)), 0d, 0d));
        //lossScorers.add(new StrangeElementScorer());
        final CommonLossEdgeScorer alesscorer = new CommonLossEdgeScorer();
        alesscorer.setRecombinator(new CommonLossEdgeScorer.LegacyOldSiriusRecombinator());

        final double GAMMA = 1d;

        for (String s : CommonLossEdgeScorer.ales_list) {
            alesscorer.addCommonLoss(MolecularFormula.parse(s), GAMMA);
        }

        alesscorer.addImplausibleLosses(Math.log(0.25));

        lossScorers.add(new ChemicalPriorEdgeScorer(new Hetero2CarbonScorer(Hetero2CarbonScorer.getHeteroToCarbonDistributionFromKEGG()), 0d, 0d));

        lossScorers.add(alesscorer);

        // peak scorers
        final List<PeakScorer> peakScorers = new ArrayList<PeakScorer>();
        peakScorers.add(new PeakIsNoiseScorer(ExponentialDistribution.getMedianEstimator()));
        peakScorers.add(new TreeSizeScorer(0d));

        // root scorers
        final List<DecompositionScorer<?>> rootScorers = new ArrayList<DecompositionScorer<?>>();
        rootScorers.add(new ChemicalPriorScorer(new Hetero2CarbonScorer(Hetero2CarbonScorer.getHeteroToCarbonDistributionFromKEGG()), 0d, 0d));
        rootScorers.add(new MassDeviationVertexScorer());

        // fragment scorers
        final List<DecompositionScorer<?>> fragmentScorers = new ArrayList<DecompositionScorer<?>>();
        fragmentScorers.add(new MassDeviationVertexScorer());
        //fragmentScorers.add(CommonFragmentsScore.getLearnedCommonFragmentScorer());

        // setup
        analysis.setLossScorers(lossScorers);
        analysis.setRootScorers(rootScorers);
        analysis.setDecompositionScorers(fragmentScorers);
        analysis.setFragmentPeakScorers(peakScorers);
        analysis.setPeakPairScorers(peakPairScorers);

        analysis.setPeakMerger(new HighIntensityMerger(0.01d));
        analysis.getPostProcessors().add(new NoiseThresholdFilter(0.005d));
        analysis.getPreprocessors().add(new NormalizeToSumPreprocessor());

        analysis.getPostProcessors().add(new LimitNumberOfPeaksFilter(40));

        //analysis.setTreeBuilder(new DPTreeBuilder(15));

        final MutableMeasurementProfile profile = new MutableMeasurementProfile();

        profile.setAllowedMassDeviation(new Deviation(10, 0.002d));
        profile.setStandardMassDifferenceDeviation(new Deviation(2.5d));
        profile.setStandardMs2MassDeviation(new Deviation(10d, 0.002d));
        profile.setStandardMs1MassDeviation(new Deviation(11d / 4d));
        profile.setFormulaConstraints(new FormulaConstraints());
        profile.setMedianNoiseIntensity(ExponentialDistribution.fromLambda(0.4d).getMedian());
        profile.setIntensityDeviation(0.02);
        analysis.setDefaultProfile(profile);

        return analysis;
    }

    /**
     * Construct a new FragmentationPatternAnaylsis with default scorers
     */
    public static FragmentationPatternAnalysis defaultAnalyzer() {
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();

        // peak pair scorers
        final LossSizeScorer lossSize = new LossSizeScorer(new LogNormalDistribution(4d, 1d), -5d);/*LossSizeScorer.LEARNED_DISTRIBUTION, LossSizeScorer.LEARNED_NORMALIZATION*/
        final List<PeakPairScorer> peakPairScorers = new ArrayList<PeakPairScorer>();
        peakPairScorers.add(new CollisionEnergyEdgeScorer(0.1, 0.8));
        peakPairScorers.add(lossSize);

        // loss scorers
        final List<LossScorer> lossScorers = new ArrayList<LossScorer>();
        lossScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet());
        lossScorers.add(new DBELossScorer());
        lossScorers.add(new PureCarbonNitrogenLossScorer());
        //lossScorers.add(new StrangeElementScorer());
        lossScorers.add(CommonLossEdgeScorer.getLossSizeCompensationForExpertList(lossSize, 0.75d).addImplausibleLosses(Math.log(0.01)));

        // peak scorers
        final List<PeakScorer> peakScorers = new ArrayList<PeakScorer>();
        peakScorers.add(new PeakIsNoiseScorer());
        peakScorers.add(new TreeSizeScorer(0d));

        // root scorers
        final List<DecompositionScorer<?>> rootScorers = new ArrayList<DecompositionScorer<?>>();
        rootScorers.add(new ChemicalPriorScorer());
        rootScorers.add(new MassDeviationVertexScorer());

        // fragment scorers
        final List<DecompositionScorer<?>> fragmentScorers = new ArrayList<DecompositionScorer<?>>();
        fragmentScorers.add(new MassDeviationVertexScorer());
        fragmentScorers.add(new CommonFragmentsScore(new HashMap<MolecularFormula, Double>()));

        // setup
        analysis.setLossScorers(lossScorers);
        analysis.setRootScorers(rootScorers);
        analysis.setDecompositionScorers(fragmentScorers);
        analysis.setFragmentPeakScorers(peakScorers);
        analysis.setPeakPairScorers(peakPairScorers);

        analysis.setPeakMerger(new HighIntensityMerger(0.01d));
        analysis.getPostProcessors().add(new NoiseThresholdFilter(0.005d));
        analysis.getPreprocessors().add(new NormalizeToSumPreprocessor());


        final TreeBuilder solver = loadTreeBuilder();
        analysis.setTreeBuilder(solver);

        final MutableMeasurementProfile profile = new MutableMeasurementProfile();
        profile.setAllowedMassDeviation(new Deviation(10));
        profile.setStandardMassDifferenceDeviation(new Deviation(7));
        profile.setStandardMs2MassDeviation(new Deviation(10));
        profile.setStandardMassDifferenceDeviation(new Deviation(5));
        profile.setFormulaConstraints(new FormulaConstraints());
        profile.setMedianNoiseIntensity(0.02);
        profile.setIntensityDeviation(0.02);
        analysis.setDefaultProfile(profile);

        return analysis;
    }

    private static TreeBuilder loadTreeBuilder() {
        try {
            // is gurobi.jar in classpath?
            final Class<TreeBuilder> kl = (Class<TreeBuilder>) ClassLoader.getSystemClassLoader().loadClass("de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver");
            // is gurobi native library in classpath?
            try {
                final TreeBuilder b = kl.newInstance();
                kl.getMethod("setNumberOfCPUs", int.class).invoke(b, Math.min(4, Runtime.getRuntime().availableProcessors()));
                return b;
            } catch (Throwable e) {
                System.err.println("Warning: Gurobi couldn't loaded: " + e.getMessage());
                return new DPTreeBuilder(12);
            }
        } catch (ClassNotFoundException e) {
            return new DPTreeBuilder(12);
        }
    }

    /**
     * Helper function to change the parameters of a specific scorer
     * <code>
     * analyzer.getByClassName(MassDeviationScorer.class, analyzer.getDecompositionScorers).setMassPenalty(4d);
     * </code>
     *
     * @param klass
     * @param list
     * @param <S>
     * @param <T>
     * @return
     */
    public static <S, T extends S> T getByClassName(Class<T> klass, List<S> list) {
        for (S elem : list) if (elem.getClass().equals(klass)) return (T) elem;
        return null;
    }

    public static <S, T extends S> T getOrCreateByClassName(Class<T> klass, List<S> list) {
        for (S elem : list) if (elem.getClass().equals(klass)) return (T) elem;
        try {
            final T obj = klass.newInstance();
            list.add(obj);
            return obj;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public <G, D, L> void writeToProfile(DataDocument<G, D, L> document, G value) {
        final ParameterHelper helper = ParameterHelper.getParameterHelper();
        final D dict = document.getDictionary(value);
        final D fpa = document.newDictionary();
        exportParameters(helper, document, fpa);
        document.addToDictionary(fpa, "$name", helper.toClassName(getClass()));
        document.addDictionaryToDictionary(dict, "FragmentationPatternAnalysis", fpa);
        if (document.hasKeyInDictionary(dict, "profile")) {
            final MeasurementProfile otherProfile = (MeasurementProfile) helper.unwrap(document, document.getFromDictionary(dict, "profile"));
            if (!otherProfile.equals(defaultProfile)) {
                if (defaultProfile != null) {
                    final D profDict = document.newDictionary();
                    defaultProfile.exportParameters(helper, document, profDict);
                    document.addDictionaryToDictionary(fpa, "default", profDict);
                }
            }
        } else if (defaultProfile != null) {
            final D profDict = document.newDictionary();
            defaultProfile.exportParameters(helper, document, profDict);
            document.addDictionaryToDictionary(dict, "profile", profDict);
        }
    }

    /**
     * Remove all scorers and set analyzer to clean state
     */
    public void setInitial() {
        this.inputValidators = new ArrayList<InputValidator>();
        inputValidators.add(new MissingValueValidator());
        this.validatorWarning = new Warning.Noop();
        this.normalizationType = NormalizationType.GLOBAL;
        this.peakMerger = new HighIntensityMerger();
        this.repairInput = true;
        this.decompositionScorers = new ArrayList<DecompositionScorer<?>>();
        this.preprocessors = new ArrayList<Preprocessor>();
        this.postProcessors = new ArrayList<PostProcessor>();
        this.rootScorers = new ArrayList<DecompositionScorer<?>>();
        this.peakPairScorers = new ArrayList<PeakPairScorer>();
        this.fragmentPeakScorers = new ArrayList<PeakScorer>();
        this.graphBuilder = new SubFormulaGraphBuilder();
        this.lossScorers = new ArrayList<LossScorer>();
        final TreeBuilder solver = loadTreeBuilder();
        setTreeBuilder(solver);

    }

    /**
     * Compute a single fragmentation tree
     *
     * @param graph      fragmentation graph from which the tree should be built
     * @param lowerbound minimal score of the tree. Higher lowerbounds may result in better runtime performance
     * @return an optimal fragmentation tree with at least lowerbound score or null, if no such tree exist
     */
    public FTree computeTree(FGraph graph, double lowerbound) {
        return computeTree(graph, lowerbound, recalibrationMethod != null);
    }

    /**
     * Compute a single fragmentation tree
     *
     * @param graph         fragmentation graph from which the tree should be built
     * @param lowerbound    minimal score of the tree. Higher lowerbounds may result in better runtime performance
     * @param recalibration if true, the tree will be recalibrated
     * @return an optimal fragmentation tree with at least lowerbound score or null, if no such tree exist
     */
    public FTree computeTree(FGraph graph, double lowerbound, boolean recalibration) {
        FTree tree = treeBuilder.buildTree(graph.getAnnotationOrThrow(ProcessedInput.class), graph, lowerbound);
        if (tree == null) return null;
        addTreeAnnotations(tree);
        if (tree != null && recalibration) tree = recalibrate(tree);
        addTreeAnnotations(tree);
        return tree;
    }

    private void addTreeAnnotations(FTree tree) {
        tree.setAnnotation(Ionization.class, tree.getAnnotationOrThrow(ProcessedInput.class).getExperimentInformation().getIonization());
        FragmentAnnotation<CollisionEnergy> ce = tree.getOrCreateFragmentAnnotation(CollisionEnergy.class);
        FragmentAnnotation<CollisionEnergy[]> ces = tree.getOrCreateFragmentAnnotation(CollisionEnergy[].class);
        FragmentAnnotation<ProcessedPeak> pp = tree.getOrCreateFragmentAnnotation(ProcessedPeak.class);
        FragmentAnnotation<Peak> p = tree.getOrCreateFragmentAnnotation(Peak.class);
        for (Fragment f : tree) {
            final ProcessedPeak peak = pp.get(f);
            ce.set(f, peak.getCollisionEnergy());
            final Set<CollisionEnergy> energies = new TreeSet<CollisionEnergy>(CollisionEnergy.getMinEnergyComparator());
            int k = 0;
            for (MS2Peak op : peak.getOriginalPeaks()) {
                energies.add(op.getSpectrum().getCollisionEnergy());
            }
            ces.set(f, energies.toArray(new CollisionEnergy[energies.size()]));
            p.set(f, new Peak(peak.getMz(), peak.getRelativeIntensity()));
        }
    }

    /**
     * Recalibrates the tree
     *
     * @return Recalibration object containing score bonus and new tree
     */
    public RecalibrationMethod.Recalibration getRecalibrationFromTree(final FTree tree, boolean force) {
        if (recalibrationMethod == null || tree == null) return null;
        else return recalibrationMethod.recalibrate(tree, new MassDeviationVertexScorer(), force);
    }

    public RecalibrationMethod.Recalibration getRecalibrationFromTree(final FTree tree) {
        return getRecalibrationFromTree(tree, false);
    }

    /**
     * Recalibrates the tree. Returns either a new recalibrated tree or the old tree with recalibrated deviations and
     * (maybe) higher scores. The FragmentationTree#getRecalibrationBonus returns the improvement of the score after
     * recalibration
     *
     * @param tree
     * @return
     */
    public FTree recalibrate(FTree tree, boolean force) {
        if (tree == null) return null;
        final TreeScoring treeScoring = tree.getAnnotationOrThrow(TreeScoring.class);
        RecalibrationMethod.Recalibration rec = getRecalibrationFromTree(tree, force);
        final UnivariateFunction func = (rec == null) ? null : rec.recalibrationFunction();
        assert (func == null || (!(func instanceof PolynomialFunction) || ((PolynomialFunction) func).degree() >= 1));
        if (rec == null || (!force && rec.getScoreBonus() <= 0)) return tree;
        double oldScore = treeScoring.getOverallScore();
        if (force || rec.shouldRecomputeTree()) {
            final FTree newTree = rec.getCorrectedTree(this, tree);
            final TreeScoring newTreeScoring = newTree.getAnnotationOrThrow(TreeScoring.class);
            if (force || newTreeScoring.getOverallScore() > treeScoring.getOverallScore()) {
                tree = newTree;
                treeScoring.setRecalibrationBonus(newTreeScoring.getOverallScore() - oldScore);
            }
        } else {
            treeScoring.setOverallScore(rec.getScoreBonus());
            treeScoring.setRecalibrationBonus(treeScoring.getOverallScore() - oldScore);
        }
        return tree;
    }

    public FTree recalibrate(FTree tree) {
        return recalibrate(tree, false);
    }

    public RecalibrationMethod getRecalibrationMethod() {
        return recalibrationMethod;
    }

    public void setRecalibrationMethod(RecalibrationMethod recalibrationMethod) {
        this.recalibrationMethod = recalibrationMethod;
    }

    /**
     * Computes a fragmentation tree
     *
     * @param graph fragmentation graph from which the tree should be built
     * @return an optimal fragmentation tree
     */
    public FTree computeTree(FGraph graph) {
        return computeTree(graph, Double.NEGATIVE_INFINITY);
    }

    public MultipleTreeComputation computeTrees(ProcessedInput input) {
        return new MultipleTreeComputation(this, input, input.getPeakAnnotationOrThrow(DecompositionList.class).get(input.getParentPeak()).getDecompositions(),
                0, Integer.MAX_VALUE, 1, recalibrationMethod != null, null);
    }

    public FGraph buildGraph(ProcessedInput input, ScoredMolecularFormula candidate) {
        // build Graph
        final FGraph graph = graphBuilder.fillGraph(
                graphBuilder.addRoot(graphBuilder.initializeEmptyGraph(input),
                        input.getParentPeak(), Collections.singletonList(candidate)));
        return scoreGraph(graph);
    }

    public FGraph buildGraph(ProcessedInput input, List<ProcessedPeak> parentPeaks, List<List<ScoredMolecularFormula>> candidatesPerParentPeak) {
        // build Graph
        FGraph graph = graphBuilder.initializeEmptyGraph(input);
        for (int i = 0; i < parentPeaks.size(); ++i) {
            graph = graphBuilder.addRoot(graph, parentPeaks.get(i), candidatesPerParentPeak.get(i));
        }
        return scoreGraph(graphBuilder.fillGraph(graph));
    }

    protected FGraph scoreGraph(FGraph graph) {
        // score graph
        final Iterator<Loss> edges = graph.lossIterator();
        final ProcessedInput input = graph.getAnnotationOrThrow(ProcessedInput.class);
        final Scoring scoring = input.getAnnotationOrThrow(Scoring.class);
        final double[] peakScores = scoring.getPeakScores();
        final double[][] peakPairScores = scoring.getPeakPairScores();
        final LossScorer[] lossScorers = this.lossScorers.toArray(new LossScorer[this.lossScorers.size()]);
        final Object[] precomputeds = new Object[lossScorers.length];
        final ScoredFormulaMap map = graph.getAnnotationOrThrow(ScoredFormulaMap.class);
        final FragmentAnnotation<ProcessedPeak> peakAno = graph.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        for (int i = 0; i < precomputeds.length; ++i) precomputeds[i] = lossScorers[i].prepare(input);
        while (edges.hasNext()) {
            final Loss loss = edges.next();
            final Fragment u = loss.getSource();
            final Fragment v = loss.getTarget();
            // take score of molecular formula
            double score = map.get(v.getFormula());
            // add it to score of the peak
            score += peakScores[peakAno.get(v).getIndex()];
            // add it to the score of the peak pairs
            if (!u.isRoot())
                score += peakPairScores[peakAno.get(u).getIndex()][peakAno.get(v).getIndex()]; // TODO: Umdrehen!
            // add the score of the loss
            if (!u.isRoot())
                for (int i = 0; i < lossScorers.length; ++i)
                    score += lossScorers[i].score(loss, input, precomputeds[i]);
            assert !Double.isInfinite(score);
            loss.setWeight(score);
        }
        if (reduction != null) reduction.reduce(graph, Double.NEGATIVE_INFINITY); // TODO: implement lowerbound
        //assert false;
        return graph;
    }

    public ProcessedInput preprocessingWithRecalibration(Ms2Experiment experiment, RecalibrationMethod.Recalibration recalibration) {
        final ProcessedInput input = preprocessWithoutDecomposing(experiment);
        final UnivariateFunction f = recalibration.recalibrationFunction();
        for (ProcessedPeak peak : input.getMergedPeaks()) {
            //peak.setOriginalMz(peak.getMz());
            peak.setMz(f.value(peak.getOriginalMz()));
        }
        // decompose and score all peaks
        return decomposeAndScore(input.getExperimentInformation(), experiment, input.getMergedPeaks());
    }

    public ProcessedInput preprocessing(Ms2Experiment experiment) {
        final ProcessedInput input = preprocessWithoutDecomposing(experiment);
        // decompose and score all peaks
        return decomposeAndScore(input.getExperimentInformation(), experiment, input.getMergedPeaks());
    }

    ProcessedInput preprocessWithoutDecomposing(Ms2Experiment experiment) {
        // use a mutable experiment, such that we can easily modify it. Validate and preprocess input
        Ms2Experiment input = wrapInput(preProcess(validate(experiment)));
        // normalize all peaks and merge peaks within the same spectrum
        // put peaks from all spectra together in a flatten list
        List<ProcessedPeak> peaks = normalize(input);
        peaks = postProcess(PostProcessor.Stage.AFTER_NORMALIZING, new ProcessedInput(input, experiment, peaks, null)).getMergedPeaks();
        // merge peaks from different spectra
        final List<ProcessedPeak> processedPeaks = mergePeaks(input, peaks);
        final ProcessedPeak parentPeak = selectParentPeakAndCleanSpectrum(input, processedPeaks);
        final List<ProcessedPeak> afterMerging =
                postProcess(PostProcessor.Stage.AFTER_MERGING, new ProcessedInput(input, experiment, processedPeaks, parentPeak)).getMergedPeaks();
        return new ProcessedInput(input, experiment, afterMerging, parentPeak);
    }

    /**
     * Scans the spectrum for the parent peak, delete all peaks with higher masses than the parent peak and
     * (noise) peaks which are near the parent peak. If there is no parent peak found, a synthetic one is created.
     * After cleaning, the processedPeaks list should contain the parent peak as last peak in the list. Furthermore,
     * is is guaranteed, that the heaviest peak in the list is always the parent peak.
     *
     * @param experiment     input data. Should be validated, normalized, merged and preprocessed
     * @param processedPeaks list of merged peaks
     * @return parent peak. Is also the last peak in the merged peaks list
     */
    ProcessedPeak selectParentPeakAndCleanSpectrum(Ms2Experiment experiment, List<ProcessedPeak> processedPeaks) {
        // and sort the resulting peaklist by mass
        Collections.sort(processedPeaks, new ProcessedPeak.MassComparator());
        // now search the parent peak. If it is not contained in the spectrum: create one!
        // delete all peaks behind the parent, such that the parent is the heaviest peak in the spectrum
        // Now we can access the parent peak by peaklist[peaklist.size-1]
        final double parentmass = experiment.getIonMass();
        final Deviation parentDeviation = experiment.getMeasurementProfile().getAllowedMassDeviation();
        for (int i = processedPeaks.size() - 1; i >= 0; --i) {
            if (!parentDeviation.inErrorWindow(parentmass, processedPeaks.get(i).getMz())) {
                if (processedPeaks.get(i).getMz() < parentmass) {
                    // parent peak is not contained. Create a synthetic one
                    final ProcessedPeak syntheticParent = new ProcessedPeak();
                    syntheticParent.setIon(experiment.getIonization());
                    syntheticParent.setMz(parentmass);
                    syntheticParent.setOriginalMz(parentmass);
                    processedPeaks.add(syntheticParent);
                    break;
                } else processedPeaks.remove(i);
            } else break;
        }
        assert parentDeviation.inErrorWindow(parentmass, processedPeaks.get(processedPeaks.size() - 1).getMz()) : "heaviest peak is parent peak";
        // the heaviest fragment that is possible is M - H
        // everything which is heavier is noise
        final double threshold = parentmass + experiment.getMeasurementProfile().getAllowedMassDeviation().absoluteFor(parentmass) - PeriodicTable.getInstance().getByName("H").getMass();
        final ProcessedPeak parentPeak = processedPeaks.get(processedPeaks.size() - 1);
        // delete all peaks between parentmass-H and parentmass except the parent peak itself
        for (int i = processedPeaks.size() - 2; i >= 0; --i) {
            if (processedPeaks.get(i).getMz() <= threshold) break;
            processedPeaks.set(processedPeaks.size() - 2, parentPeak);
            processedPeaks.remove(processedPeaks.size() - 1);
        }
        return parentPeak;
    }

    ProcessedInput decomposeAndScore(Ms2Experiment experiment, Ms2Experiment originalExperiment, List<ProcessedPeak> processedPeaks) {
        final Deviation parentDeviation = experiment.getMeasurementProfile().getAllowedMassDeviation();
        // sort again...
        processedPeaks = new ArrayList<ProcessedPeak>(processedPeaks);
        Collections.sort(processedPeaks, new ProcessedPeak.MassComparator());
        final ProcessedPeak parentPeak = processedPeaks.get(processedPeaks.size() - 1);
        // decompose peaks
        final FormulaConstraints constraints = experiment.getMeasurementProfile().getFormulaConstraints();
        final MassToFormulaDecomposer decomposer = decomposers.getDecomposer(constraints.getChemicalAlphabet());
        final Ionization ion = experiment.getIonization();
        final Deviation fragmentDeviation = experiment.getMeasurementProfile().getAllowedMassDeviation();
        final List<MolecularFormula> pmds = decomposer.decomposeToFormulas(parentPeak.getUnmodifiedOriginalMass(), parentDeviation, constraints);
        final ArrayList<List<MolecularFormula>> decompositions = new ArrayList<List<MolecularFormula>>(processedPeaks.size());
        int j = 0;
        for (ProcessedPeak peak : processedPeaks.subList(0, processedPeaks.size() - 1)) {
            peak.setIndex(j++);
            decompositions.add(decomposer.decomposeToFormulas(peak.getUnmodifiedMass(), fragmentDeviation, constraints));
        }
        parentPeak.setIndex(processedPeaks.size() - 1);
        assert parentPeak == processedPeaks.get(processedPeaks.size() - 1);
        // important: for each two peaks which are within 2*massrange:
        //  => make decomposition list disjoint
        final Deviation window = fragmentDeviation.multiply(2);
        for (int i = 1; i < processedPeaks.size() - 1; ++i) {
            if (window.inErrorWindow(processedPeaks.get(i).getMz(), processedPeaks.get(i - 1).getMz())) {
                final HashSet<MolecularFormula> right = new HashSet<MolecularFormula>(decompositions.get(i));
                final ArrayList<MolecularFormula> left = new ArrayList<MolecularFormula>(decompositions.get(i - 1));
                final double leftMass = ion.subtractFromMass(processedPeaks.get(i - 1).getMass());
                final double rightMass = ion.subtractFromMass(processedPeaks.get(i).getMass());
                final Iterator<MolecularFormula> leftIter = left.iterator();
                while (leftIter.hasNext()) {
                    final MolecularFormula leftFormula = leftIter.next();
                    if (right.contains(leftFormula)) {
                        if (Math.abs(leftFormula.getMass() - leftMass) < Math.abs(leftFormula.getMass() - rightMass)) {
                            right.remove(leftFormula);
                        } else {
                            leftIter.remove();
                        }
                    }
                }
                decompositions.set(i - 1, left);
                decompositions.set(i, new ArrayList<MolecularFormula>(right));
            }
        }
        final ProcessedInput preprocessed = new ProcessedInput(experiment, originalExperiment, processedPeaks, parentPeak);
        final int n = processedPeaks.size();
        preprocessed.getOrCreateAnnotation(Scoring.class).initializeScoring(n);
        // score peak pairs
        final double[][] peakPairScores = preprocessed.getAnnotationOrThrow(Scoring.class).getPeakPairScores();
        for (PeakPairScorer scorer : peakPairScorers) {
            scorer.score(processedPeaks, preprocessed, peakPairScores);
        }
        // score fragment peaks
        final double[] peakScores = preprocessed.getAnnotationOrThrow(Scoring.class).getPeakScores();
        for (PeakScorer scorer : fragmentPeakScorers) {
            scorer.score(processedPeaks, preprocessed, peakScores);
        }
        // dont score parent peak
        peakScores[peakScores.length - 1] = 0d;

        final PeakAnnotation<DecompositionList> decompositionList = preprocessed.getOrCreatePeakAnnotation(DecompositionList.class);
        // score peaks
        {
            final ArrayList<Object> preparations = new ArrayList<Object>(decompositionScorers.size());
            for (DecompositionScorer<?> scorer : decompositionScorers) preparations.add(scorer.prepare(preprocessed));
            for (int i = 0; i < processedPeaks.size() - 1; ++i) {
                final List<MolecularFormula> decomps = decompositions.get(i);
                final ArrayList<ScoredMolecularFormula> scored = new ArrayList<ScoredMolecularFormula>(decomps.size());
                for (MolecularFormula f : decomps) {
                    double score = 0d;
                    int k = 0;
                    for (DecompositionScorer<?> scorer : decompositionScorers) {
                        score += ((DecompositionScorer<Object>) scorer).score(f, processedPeaks.get(i), preprocessed, preparations.get(k++));
                    }
                    scored.add(new ScoredMolecularFormula(f, score));
                }
                decompositionList.set(processedPeaks.get(i), new DecompositionList(scored));
            }
        }
        // same with root
        {
            final ArrayList<Object> preparations = new ArrayList<Object>(rootScorers.size());
            for (DecompositionScorer<?> scorer : rootScorers) preparations.add(scorer.prepare(preprocessed));
            final ArrayList<ScoredMolecularFormula> scored = new ArrayList<ScoredMolecularFormula>(pmds.size());
            for (MolecularFormula f : pmds) {
                double score = 0d;
                int k = 0;
                for (DecompositionScorer<?> scorer : rootScorers) {
                    score += ((DecompositionScorer<Object>) scorer).score(f, parentPeak, preprocessed, preparations.get(k++));
                }
                scored.add(new ScoredMolecularFormula(f, score));

            }
            Collections.sort(scored, Collections.reverseOrder());
            decompositionList.set(parentPeak, new DecompositionList(scored));
            preprocessed.addAnnotation(DecompositionList.class, decompositionList.get(parentPeak));
        }
        // set peak indizes
        for (int i = 0; i < processedPeaks.size(); ++i) processedPeaks.get(i).setIndex(i);

        // final processing
        return postProcess(PostProcessor.Stage.AFTER_DECOMPOSING, preprocessed);
    }

    /*

    Merging:
        - 1. lösche alle Peaks die zu nahe an einem anderen Peak im selben Spektrum sind un geringe Intensität
        - 2. der Peakmerger bekommt nur Peak aus unterschiedlichen Spektren und mergt diese
        - 3. Nach der Decomposition läuft man alle peaks in der Liste durch. Wenn zwischen zwei
             Peaks der Abstand zu klein wird, werden diese Peaks disjunkt, in dem die doppelt vorkommenden
             Decompositions auf einen peak (den mit der geringeren asseabweichung) eindeutig verteilt werden.

     */

    /**
     * a set of peaks are merged if:
     * - they are from different spectra
     * - they they are in the same mass range
     *
     * @param experiment
     * @param peaklists  a peaklist for each spectrum
     * @return a list of merged peaks
     */
    ArrayList<ProcessedPeak> mergePeaks(Ms2Experiment experiment, List<ProcessedPeak> peaklists) {
        final ArrayList<ProcessedPeak> mergedPeaks = new ArrayList<ProcessedPeak>(peaklists.size());
        peakMerger.mergePeaks(peaklists, experiment, experiment.getMeasurementProfile().getAllowedMassDeviation(), new Merger() {
            @Override
            public ProcessedPeak merge(List<ProcessedPeak> peaks, int index, double newMz) {
                final ProcessedPeak newPeak = peaks.get(index);
                // sum up global intensities, take maximum of local intensities
                double local = 0d, global = 0d, relative = 0d;
                for (ProcessedPeak p : peaks) {
                    local = Math.max(local, p.getLocalRelativeIntensity());
                    global += p.getGlobalRelativeIntensity();
                    relative += p.getRelativeIntensity();
                }
                newPeak.setMz(newMz);
                newPeak.setLocalRelativeIntensity(local);
                newPeak.setGlobalRelativeIntensity(global);
                newPeak.setRelativeIntensity(relative);
                final MS2Peak[] originalPeaks = new MS2Peak[peaks.size()];
                for (int i = 0; i < peaks.size(); ++i) originalPeaks[i] = peaks.get(i).getOriginalPeaks().get(0);
                newPeak.setOriginalPeaks(Arrays.asList(originalPeaks));
                mergedPeaks.add(newPeak);
                return newPeak;
            }
        });
        return mergedPeaks;
    }

    ArrayList<ProcessedPeak> normalize(Ms2Experiment experiment) {
        final double parentMass = experiment.getIonMass();
        final ArrayList<ProcessedPeak> peaklist = new ArrayList<ProcessedPeak>(100);
        final Deviation mergeWindow = experiment.getMeasurementProfile().getAllowedMassDeviation().divide(2d);
        final Ionization ion = experiment.getIonization();
        double globalMaxIntensity = 0d;
        for (Ms2Spectrum s : experiment.getMs2Spectra()) {
            // merge peaks: iterate them from highest to lowest intensity and remove peaks which
            // are in the mass range of a high intensive peak
            final MutableSpectrum<Peak> sortedByIntensity = new SimpleMutableSpectrum(s);
            Spectrums.sortSpectrumByDescendingIntensity(sortedByIntensity);
            // simple spectra are always ordered by mass
            final SimpleSpectrum sortedByMass = new SimpleSpectrum(s);
            final BitSet deletedPeaks = new BitSet(s.size());
            for (int i = 0; i < s.size(); ++i) {
                // get index of peak in mass-ordered spectrum
                final double mz = sortedByIntensity.getMzAt(i);
                final int index = Spectrums.binarySearch(sortedByMass, mz);
                assert index >= 0;
                if (deletedPeaks.get(index)) continue; // peak is already deleted
                // delete all peaks within the mass range
                for (int j = index - 1; j >= 0 && mergeWindow.inErrorWindow(mz, sortedByMass.getMzAt(j)); --j)
                    deletedPeaks.set(j, true);
                for (int j = index + 1; j < s.size() && mergeWindow.inErrorWindow(mz, sortedByMass.getMzAt(j)); ++j)
                    deletedPeaks.set(j, true);
            }
            final int offset = peaklist.size();
            // add all remaining peaks to the peaklist
            for (int i = 0; i < s.size(); ++i) {
                if (!deletedPeaks.get(i)) {
                    final ProcessedPeak propeak = new ProcessedPeak(new MS2Peak(s, sortedByMass.getMzAt(i), sortedByMass.getIntensityAt(i)));
                    propeak.setIon(ion);
                    peaklist.add(propeak);

                }
            }
            // now normalize spectrum. Ignore peaks near to the parent peak
            final double lowerbound = parentMass - 0.1d;
            double scale = 0d;
            for (int i = offset; i < peaklist.size() && peaklist.get(i).getMz() < lowerbound; ++i) {
                scale = Math.max(scale, peaklist.get(i).getIntensity());
            }
            // now set local relative intensities
            for (int i = offset; i < peaklist.size(); ++i) {
                final ProcessedPeak peak = peaklist.get(i);
                peak.setLocalRelativeIntensity(peak.getIntensity() / scale);
            }
            // and adjust global relative intensity
            globalMaxIntensity = Math.max(globalMaxIntensity, scale);
        }
        // now calculate global normalized intensities
        for (ProcessedPeak peak : peaklist) {
            peak.setGlobalRelativeIntensity(peak.getIntensity() / globalMaxIntensity);
            peak.setRelativeIntensity(normalizationType == NormalizationType.GLOBAL ? peak.getGlobalRelativeIntensity() : peak.getLocalRelativeIntensity());
        }
        // finished!
        return peaklist;
    }

    Ms2Experiment preProcess(Ms2Experiment experiment) {
        for (Preprocessor proc : preprocessors) {
            experiment = proc.process(experiment);
        }
        return experiment;
    }

    ProcessedInput postProcess(PostProcessor.Stage stage, ProcessedInput input) {
        for (PostProcessor proc : postProcessors) {
            if (proc.getStage() == stage) {
                input = proc.process(input);
            }
        }
        return input;
    }

    public Ms2Experiment validate(Ms2Experiment experiment) {
        // first of all: insert default profile if no profile is given
        Ms2ExperimentImpl input = new Ms2ExperimentImpl(experiment);
        if (input.getMeasurementProfile() == null)
            input.setMeasurementProfile(new MutableMeasurementProfile(defaultProfile));
        else
            input.setMeasurementProfile(MutableMeasurementProfile.merge(defaultProfile, input.getMeasurementProfile()));
        experiment = input;
        for (InputValidator validator : inputValidators) {
            experiment = validator.validate(experiment, validatorWarning, repairInput);
        }
        return experiment;
    }

    protected Ms2ExperimentImpl wrapInput(Ms2Experiment exp) {
        if (exp instanceof Ms2ExperimentImpl) return (Ms2ExperimentImpl) exp;
        else return new Ms2ExperimentImpl(exp);
    }

    //////////////////////////////////////////
    //        GETTER/SETTER
    //////////////////////////////////////////


    public List<InputValidator> getInputValidators() {
        return inputValidators;
    }

    public void setInputValidators(List<InputValidator> inputValidators) {
        this.inputValidators = inputValidators;
    }

    public Warning getValidatorWarning() {
        return validatorWarning;
    }

    public void setValidatorWarning(Warning validatorWarning) {
        this.validatorWarning = validatorWarning;
    }

    public boolean isRepairInput() {
        return repairInput;
    }

    public void setRepairInput(boolean repairInput) {
        this.repairInput = repairInput;
    }

    public NormalizationType getNormalizationType() {
        return normalizationType;
    }

    public void setNormalizationType(NormalizationType normalizationType) {
        this.normalizationType = normalizationType;
    }

    public PeakMerger getPeakMerger() {
        return peakMerger;
    }

    public void setPeakMerger(PeakMerger peakMerger) {
        this.peakMerger = peakMerger;
    }

    public List<DecompositionScorer<?>> getDecompositionScorers() {
        return decompositionScorers;
    }

    public void setDecompositionScorers(List<DecompositionScorer<?>> decompositionScorers) {
        this.decompositionScorers = decompositionScorers;
    }

    public List<DecompositionScorer<?>> getRootScorers() {
        return rootScorers;
    }

    public void setRootScorers(List<DecompositionScorer<?>> rootScorers) {
        this.rootScorers = rootScorers;
    }

    public List<LossScorer> getLossScorers() {
        return lossScorers;
    }

    public void setLossScorers(List<LossScorer> lossScorers) {
        this.lossScorers = lossScorers;
    }

    public List<PeakPairScorer> getPeakPairScorers() {
        return peakPairScorers;
    }

    public void setPeakPairScorers(List<PeakPairScorer> peakPairScorers) {
        this.peakPairScorers = peakPairScorers;
    }

    public List<PeakScorer> getFragmentPeakScorers() {
        return fragmentPeakScorers;
    }

    public void setFragmentPeakScorers(List<PeakScorer> fragmentPeakScorers) {
        this.fragmentPeakScorers = fragmentPeakScorers;
    }

    public List<Preprocessor> getPreprocessors() {
        return preprocessors;
    }

    public void setPreprocessors(List<Preprocessor> preprocessors) {
        this.preprocessors = preprocessors;
    }

    public List<PostProcessor> getPostProcessors() {
        return postProcessors;
    }

    public void setPostProcessors(List<PostProcessor> postProcessors) {
        this.postProcessors = postProcessors;
    }

    public TreeBuilder getTreeBuilder() {
        return treeBuilder;
    }

    public void setTreeBuilder(TreeBuilder treeBuilder) {
        this.treeBuilder = treeBuilder;
    }

    public MutableMeasurementProfile getDefaultProfile() {
        return defaultProfile;
    }

    public void setDefaultProfile(MeasurementProfile defaultProfile) {
        this.defaultProfile = new MutableMeasurementProfile(defaultProfile);
    }

    public MassToFormulaDecomposer getDecomposerFor(ChemicalAlphabet alphabet) {
        return decomposers.getDecomposer(alphabet);
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        setInitial();
        fillList(preprocessors, helper, document, dictionary, "preProcessing");
        fillList(postProcessors, helper, document, dictionary, "postProcessing");
        fillList(rootScorers, helper, document, dictionary, "rootScorers");
        fillList(decompositionScorers, helper, document, dictionary, "fragmentScorers");
        fillList(fragmentPeakScorers, helper, document, dictionary, "peakScorers");
        fillList(peakPairScorers, helper, document, dictionary, "peakPairScorers");
        fillList(lossScorers, helper, document, dictionary, "lossScorers");
        peakMerger = (PeakMerger) helper.unwrap(document, document.getFromDictionary(dictionary, "merge"));
        if (document.hasKeyInDictionary(dictionary, "recalibrationMethod")) {
            recalibrationMethod = (RecalibrationMethod) helper.unwrap(document, document.getFromDictionary(dictionary, "recalibrationMethod"));
        } else recalibrationMethod = null;
        if (document.hasKeyInDictionary(dictionary, "default"))
            defaultProfile = new MutableMeasurementProfile((MeasurementProfile) helper.unwrap(document, document.getFromDictionary(dictionary, "default")));
        else
            defaultProfile = null;

    }

    private <T, G, D, L> void fillList(List<T> list, ParameterHelper helper, DataDocument<G, D, L> document, D dictionary, String keyName) {
        Iterator<G> ls = document.iteratorOfList(document.getListFromDictionary(dictionary, keyName));
        while (ls.hasNext()) {
            final G l = ls.next();
            list.add((T) helper.unwrap(document, l));
        }
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        exportParameters(helper, document, dictionary, true);
    }

    protected <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary, boolean withProfile) {
        L list = document.newList();
        for (Preprocessor p : preprocessors) document.addToList(list, helper.wrap(document, p));
        document.addListToDictionary(dictionary, "preProcessing", list);
        list = document.newList();
        for (PostProcessor p : postProcessors) document.addToList(list, helper.wrap(document, p));
        document.addListToDictionary(dictionary, "postProcessing", list);
        list = document.newList();
        for (DecompositionScorer s : rootScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "rootScorers", list);
        list = document.newList();
        for (DecompositionScorer s : decompositionScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "fragmentScorers", list);
        list = document.newList();
        for (PeakScorer s : fragmentPeakScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "peakScorers", list);
        list = document.newList();
        for (PeakPairScorer s : peakPairScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "peakPairScorers", list);
        list = document.newList();
        for (LossScorer s : lossScorers) document.addToList(list, helper.wrap(document, s));
        document.addListToDictionary(dictionary, "lossScorers", list);
        if (recalibrationMethod != null)
            document.addToDictionary(dictionary, "recalibrationMethod", helper.wrap(document, recalibrationMethod));
        document.addToDictionary(dictionary, "merge", helper.wrap(document, peakMerger));
        if (withProfile)
            document.addToDictionary(dictionary, "default", helper.wrap(document, new MutableMeasurementProfile(defaultProfile)));

    }
}
