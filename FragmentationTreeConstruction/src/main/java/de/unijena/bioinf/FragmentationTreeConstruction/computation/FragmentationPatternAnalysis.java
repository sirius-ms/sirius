package de.unijena.bioinf.FragmentationTreeConstruction.computation;


import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter;
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer;
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.GraphBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.SubFormulaGraphBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.InputValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.MissingValueValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.Warning;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.HighIntensityMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.Merger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.PeakMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.DPTreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.MassDecomposer.Chemistry.DecomposerCache;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;

import java.util.*;

public class FragmentationPatternAnalysis {

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
    private MeasurementProfile defaultProfile;

    /**
     * Construct a new FragmentationPatternAnaylsis with default scorers
     */
    public static FragmentationPatternAnalysis defaultAnalyzer() {
        final FragmentationPatternAnalysis analysis = new FragmentationPatternAnalysis();
        analysis.setToSirius();
        final List<LossScorer> lossScorers = new ArrayList<LossScorer>();
        lossScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet());
        lossScorers.add(new DBELossScorer());
        lossScorers.add(new PureCarbonNitrogenLossScorer());

        lossScorers.add(new StrangeElementScorer());

        //lossScorers.add(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1)));
        lossScorers.add(CommonLossEdgeScorer.getOptimizedCommonLossScorer().recombinateSpec(2, new StrangeElementScorer()).merge(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1))));

        final List<PeakPairScorer> peakPairScorers = new ArrayList<PeakPairScorer>();
        peakPairScorers.add(new CollisionEnergyEdgeScorer(0.1, 0.8));


        //peakPairScorers.add(new LossSizeScorer(LogNormalDistribution.withMeanAndSd(3.8656329978554234d, 0.5512475076353699d), -4.355600412857635d));
        peakPairScorers.add(new LossSizeScorer(LossSizeScorer.LEARNED_DISTRIBUTION, LossSizeScorer.LEARNED_NORMALIZATION));

        final double lambda = 1d;
        final double massDev = 5;

        getByClassName(MassDeviationVertexScorer.class, analysis.getDecompositionScorers()).setMassPenalty(massDev);

        analysis.setPeakPairScorers(peakPairScorers);

        /*
        analysis.getDecompositionScorers().add(new ChemicalPriorScorer(ChemicalCompoundScorer.createDefaultCompoundScorer(true),
                ChemicalPriorScorer.LEARNED_NORMALIZATION_CONSTANT, 100d)
        );
        */
        analysis.getDecompositionScorers().add(CommonFragmentsScore.getLearnedCommonFragmentScorer());
        analysis.getFragmentPeakScorers().add(new TreeSizeScorer(2d));

        analysis.setLossScorers(lossScorers);
        analysis.setPeakMerger(new HighIntensityMerger(0.01d));
        analysis.getPostProcessors().add(new NoiseThresholdFilter(0.01d));
        analysis.getPostProcessors().add(new LimitNumberOfPeaksFilter(50));
        analysis.setTreeBuilder(new GurobiSolver());
        getByClassName(PeakIsNoiseScorer.class, analysis.getFragmentPeakScorers()).setDistribution(new ParetoDistribution(lambda, 0.005d));
        final GurobiSolver solver = new GurobiSolver();
        solver.setNumberOfCPUs(Runtime.getRuntime().availableProcessors());
        analysis.setTreeBuilder(solver);
        return analysis;
    }

    /**
     * Helper function to change the parameters of a specific scorer
     * <code>
     * analyzer.getByClassName(MassDeviationScorer.class, analyzer.getDecompositionScorers).setMassPenalty(4d);
     * </code>
     * @param klass
     * @param list
     * @param <S>
     * @param <T>
     * @return
     */
    public static <S, T extends S> T getByClassName(Class<T> klass, List<S> list) {
        for (S elem : list) if (elem.getClass().equals(klass)) return (T)elem;
        return null;
    }

    public FragmentationPatternAnalysis() {
        this.decomposers = new DecomposerCache();
        setToDefault();
    }

    /**
     * Remove all scorers and set analyzer to clean state
     */
    public void setInitial() {
        this.inputValidators = new ArrayList<InputValidator>();
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
        this.treeBuilder = new DPTreeBuilder(16);
    }

    public void setToDefault() {
        setInitial();
        inputValidators.add(new MissingValueValidator());
        decompositionScorers.add(new MassDeviationVertexScorer(false));
        decompositionScorers.add(CommonFragmentsScore.getLearnedCommonFragmentScorer(4).addLosses(CommonLossEdgeScorer.getDefaultCommonLossScorer(1).getMap()).useHTolerance());
        rootScorers.add(new MassDeviationVertexScorer(true));
        rootScorers.add(new ChemicalPriorScorer());
        fragmentPeakScorers.add(new PeakIsNoiseScorer(4));
        lossScorers.add(CommonLossEdgeScorer.getDefaultCommonLossScorer(1).recombinate(3).merge(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1))));
        lossScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet(Math.log(0.9), Math.log(0.1), 0d));
        lossScorers.add(new DBELossScorer());
        peakPairScorers.add(new CollisionEnergyEdgeScorer(0.1, 0.8));
        peakPairScorers.add(new RelativeLossSizeScorer());
        final List<FormulaFilter> formulaFilters = new ArrayList<FormulaFilter>();
        formulaFilters.add(new ValenceFilter());
        defaultProfile = new MutableMeasurementProfile(new Deviation(10), new Deviation(10).divide(3), new Deviation(10).divide(3),
                new Deviation(10).divide(4), new FormulaConstraints(new ChemicalAlphabet(), formulaFilters), 0.02d, 0.02d);
    }

    public void setToSirius() {
        setInitial();
        inputValidators.add(new MissingValueValidator());
        decompositionScorers.add(new MassDeviationVertexScorer(false));
        rootScorers.add(new MassDeviationVertexScorer(true));
        rootScorers.add(new ChemicalPriorScorer(ChemicalCompoundScorer.createDefaultCompoundScorer(), ChemicalPriorScorer.LEARNED_NORMALIZATION_CONSTANT_FOR_ROOT));
        fragmentPeakScorers.add(new PeakIsNoiseScorer(4));
        lossScorers.add(CommonLossEdgeScorer.getDefaultCommonLossScorer(1).recombinate(3).merge(CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(Math.log(0.1))));
        lossScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet());
        lossScorers.add(new DBELossScorer());
        lossScorers.add(new PureCarbonNitrogenLossScorer());
        peakPairScorers.add(new CollisionEnergyEdgeScorer(0.1, 0.8));
        peakPairScorers.add(new RelativeLossSizeScorer());
    }

    /**
     * Compute a single fragmentation tree
     * @param graph fragmentation graph from which the tree should be built
     * @param lowerbound minimal score of the tree. Higher lowerbounds may result in better runtime performance
     * @return an optimal fragmentation tree with at least lowerbound score or null, if no such tree exist
     */
    public FragmentationTree computeTree(FragmentationGraph graph, double lowerbound) {
        return treeBuilder.buildTree(graph.getProcessedInput(), graph, lowerbound);
    }

    /**
     * Computes a fragmentation tree
     * @param graph fragmentation graph from which the tree should be built
     * @return an optimal fragmentation tree
     */
    public FragmentationTree computeTree(FragmentationGraph graph) {
        return computeTree(graph, Double.NEGATIVE_INFINITY);
    }

    public MultipleTreeComputation computeTrees(ProcessedInput input) {
        return new MultipleTreeComputation(this, input, input.getParentMassDecompositions(), 0, Integer.MAX_VALUE, 1 );
    }

    public FragmentationGraph buildGraph(ProcessedInput input, ScoredMolecularFormula candidate) {
        // build Graph
        final FragmentationGraph graph = graphBuilder.buildGraph(input, candidate);
        // score graph
        final Iterator<Loss> edges = graph.lossIterator();
        final double[] peakScores = input.getPeakScores();
        final double[][] peakPairScores = input.getPeakPairScores();
        final LossScorer[] lossScorers = this.lossScorers.toArray(new LossScorer[this.lossScorers.size()]);
        final Object[] precomputeds = new Object[lossScorers.length];
        for (int i=0; i < precomputeds.length; ++i) precomputeds[i] = lossScorers[i].prepare(input);
        while (edges.hasNext()) {
            final Loss loss = edges.next();
            final Fragment u = loss.getHead();
            final Fragment v = loss.getTail();
            // take score of molecular formula
            double score = v.getDecomposition().getScore();
            // add it to score of the peak
            score += peakScores[v.getPeak().getIndex()];
            // add it to the score of the peak pairs
            score += peakPairScores[u.getPeak().getIndex()][v.getPeak().getIndex()]; // TODO: Umdrehen!
            // add the score of the loss
            for (int i=0; i < lossScorers.length; ++i) score += lossScorers[i].score(loss, input, precomputeds[i]);
            loss.setWeight(score);
        }
        // set root scores
        graph.setRootScore(candidate.getScore() + peakScores[peakScores.length - 1]);
        graph.prepareForTreeComputation();
        return graph;
    }

    public ProcessedInput preprocessing(Ms2Experiment experiment) {
        // first of all: insert default profile if no profile is given
        Ms2ExperimentImpl input = wrapInput(experiment);
        if (input.getMeasurementProfile()==null) input.setMeasurementProfile(defaultProfile);
        else MutableMeasurementProfile.merge(defaultProfile, input.getMeasurementProfile());
        // use a mutable experiment, such that we can easily modify it. Validate and preprocess input
        input = wrapInput(preProcess(validate(input)));
        // normalize all peaks and merge peaks within the same spectrum
        // put peaks from all spectra together in a flatten list
        List<ProcessedPeak> peaks = normalize(input);
        peaks = postProcess(PostProcessor.Stage.AFTER_NORMALIZING, new ProcessedInput(input, peaks, null, null)).getMergedPeaks();
        // merge peaks from different spectra
        final List<ProcessedPeak> processedPeaks = mergePeaks(input, peaks);
        final ProcessedPeak parentPeak = selectParentPeakAndCleanSpectrum(input, processedPeaks);
        final List<ProcessedPeak> afterMerging =
                postProcess(PostProcessor.Stage.AFTER_MERGING, new ProcessedInput(input, processedPeaks, parentPeak, null)).getMergedPeaks();
        // decompose and score all peaks
        return decomposeAndScore(input, afterMerging);
    }

    /**
     * Scans the spectrum for the parent peak, delete all peaks with higher masses than the parent peak and
     * (noise) peaks which are near the parent peak. If there is no parent peak found, a synthetic one is created.
     * After cleaning, the processedPeaks list should contain the parent peak as last peak in the list. Furthermore,
     * is is guaranteed, that the heaviest peak in the list is always the parent peak.
     * @param experiment input data. Should be validated, normalized, merged and preprocessed
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
        for (int i=processedPeaks.size()-1; i >= 0; --i) {
            if (!parentDeviation.inErrorWindow(parentmass, processedPeaks.get(i).getMz())) {
                if (processedPeaks.get(i).getMz() < parentmass) {
                    // parent peak is not contained. Create a synthetic one
                    final ProcessedPeak syntheticParent = new ProcessedPeak();
                    syntheticParent.setIon(experiment.getIonization());
                    syntheticParent.setMz(parentmass);
                    processedPeaks.add(syntheticParent);
                    break;
                } else processedPeaks.remove(i);
            } else break;
        }
        assert parentDeviation.inErrorWindow(parentmass, processedPeaks.get(processedPeaks.size()-1).getMz()) : "heaviest peak is parent peak";
        // the heaviest fragment that is possible is M - H
        // everything which is heavier is noise
        final double threshold = parentmass + experiment.getMeasurementProfile().getAllowedMassDeviation().absoluteFor(parentmass) - PeriodicTable.getInstance().getByName("H").getMass();
        final ProcessedPeak parentPeak = processedPeaks.get(processedPeaks.size()-1);
        // delete all peaks between parentmass-H and parentmass except the parent peak itself
        for (int i = processedPeaks.size()-2; i >= 0; --i) {
            if (processedPeaks.get(i).getMz() <= threshold) break;
            processedPeaks.set(processedPeaks.size() - 2, parentPeak);
            processedPeaks.remove(processedPeaks.size()-1);
        }
        return parentPeak;
    }

    ProcessedInput decomposeAndScore(Ms2Experiment experiment, List<ProcessedPeak> processedPeaks) {
        final Deviation parentDeviation = experiment.getMeasurementProfile().getAllowedMassDeviation();
        // sort again...
        processedPeaks = new ArrayList<ProcessedPeak>(processedPeaks);
        Collections.sort(processedPeaks, new ProcessedPeak.MassComparator());
        final ProcessedPeak parentPeak = processedPeaks.get(processedPeaks.size()-1);
        // decompose peaks
        final FormulaConstraints constraints = experiment.getMeasurementProfile().getFormulaConstraints();
        final MassToFormulaDecomposer decomposer = decomposers.getDecomposer(constraints.getChemicalAlphabet());
        final Ionization ion = experiment.getIonization();
        final Deviation fragmentDeviation = experiment.getMeasurementProfile().getAllowedMassDeviation();
        final List<MolecularFormula> pmds = decomposer.decomposeToFormulas(parentPeak.getUnmodifiedMass(), parentDeviation, constraints);
        final ArrayList<List<MolecularFormula>> decompositions = new ArrayList<List<MolecularFormula>>(processedPeaks.size());
        int j=0;
        for (ProcessedPeak peak : processedPeaks.subList(0, processedPeaks.size()-1)) {
            peak.setIndex(j++);
            decompositions.add(decomposer.decomposeToFormulas(peak.getUnmodifiedMass(), fragmentDeviation, constraints));
        }
        parentPeak.setIndex(processedPeaks.size()-1);
        assert parentPeak == processedPeaks.get(processedPeaks.size()-1);
        // important: for each two peaks which are within 2*massrange:
        //  => make decomposition list disjoint
        final Deviation window = fragmentDeviation.multiply(2);
        for (int i=1; i < processedPeaks.size()-1; ++i) {
            if (window.inErrorWindow(processedPeaks.get(i).getMz(), processedPeaks.get(i-1).getMz())) {
                final HashSet<MolecularFormula> right = new HashSet<MolecularFormula>(decompositions.get(i));
                final ArrayList<MolecularFormula> left = new ArrayList<MolecularFormula>(decompositions.get(i-1));
                final double leftMass = ion.subtractFromMass(processedPeaks.get(i-1).getMass());
                final double rightMass = ion.subtractFromMass(processedPeaks.get(i).getMass());
                final Iterator<MolecularFormula> leftIter = left.iterator();
                while (leftIter.hasNext()) {
                    final MolecularFormula leftFormula = leftIter.next();
                    if (right.contains(leftFormula)) {
                        if (Math.abs(leftFormula.getMass()-leftMass) < Math.abs(leftFormula.getMass()-rightMass)) {
                            right.remove(leftFormula);
                        } else {
                            leftIter.remove();
                        }
                    }
                }
                decompositions.set(i-1, left);
                decompositions.set(i, new ArrayList<MolecularFormula>(right));
            }
        }
        final ProcessedInput preprocessed = new ProcessedInput(experiment, processedPeaks, parentPeak, null);
        final int n = processedPeaks.size();
        // score peak pairs
        final double[][] peakPairScores = new double[n][n];
        for (PeakPairScorer scorer : peakPairScorers) {
            scorer.score(processedPeaks, preprocessed, peakPairScores);
        }
        // score fragment peaks
        final double[] peakScores = new double[n];
        for (PeakScorer scorer : fragmentPeakScorers) {
            scorer.score(processedPeaks, preprocessed, peakScores);
        }
        // dont score parent peak
        peakScores[peakScores.length-1]=0d;
        // score peaks
        {
            final ArrayList<Object> preparations = new ArrayList<Object>(decompositionScorers.size());
            for (DecompositionScorer<?> scorer : decompositionScorers) preparations.add(scorer.prepare(preprocessed));
            for (int i=0; i < processedPeaks.size()-1; ++i) {
                final List<MolecularFormula> decomps = decompositions.get(i);
                final ArrayList<ScoredMolecularFormula> scored = new ArrayList<ScoredMolecularFormula>(decomps.size());
                for (MolecularFormula f : decomps) {
                    double score = 0d;
                    int k=0;
                    for (DecompositionScorer<?> scorer : decompositionScorers) {
                        score += ((DecompositionScorer<Object>)scorer).score(f, processedPeaks.get(i), preprocessed, preparations.get(k++));
                    }
                    scored.add(new ScoredMolecularFormula(f, score));
                }
                processedPeaks.get(i).setDecompositions(scored);
            }
        }
        // same with root
        {
            final ArrayList<Object> preparations = new ArrayList<Object>(rootScorers.size());
            for (DecompositionScorer<?> scorer : rootScorers) preparations.add(scorer.prepare(preprocessed));
            final ArrayList<ScoredMolecularFormula> scored = new ArrayList<ScoredMolecularFormula>(pmds.size());
            for (MolecularFormula f : pmds) {
                double score = 0d;
                int k=0;
                for (DecompositionScorer<?> scorer : rootScorers) {
                    score += ((DecompositionScorer<Object>)scorer).score(f, parentPeak, preprocessed, preparations.get(k++));
                }
                scored.add(new ScoredMolecularFormula(f, score));

            }
            parentPeak.setDecompositions(scored);
        }
        // set peak indizes
        for (int i=0; i < processedPeaks.size(); ++i) processedPeaks.get(i).setIndex(i);

        final ProcessedInput processedInput =
                new ProcessedInput(experiment, processedPeaks, parentPeak, parentPeak.getDecompositions(), peakScores, peakPairScores);
        // final processing
        return postProcess(PostProcessor.Stage.AFTER_DECOMPOSING, processedInput);
    }

    /*

    Merging:
        - 1. lösche alle Peaks die zu nahe an einem anderen Peak im selben Spektrum sind un geringe Intensität
        - 2. der Peakmerger bekommt nur Peak aus unterschiedlichen Spektren und mergt diese
        - 3. Nach der Decomposition läuft man alle peaks in der Liste durch. Wenn zwischen zwei
             Peaks der Abstand zu klein wird, werden diese Peaks disjunkt, in dem die doppelt vorkommenden
             Decompositions auf einen peak (den mit der geringeren Masseabweichung) eindeutig verteilt werden.

     */

    /**
     * a set of peaks are merged if:
     * - they are from different spectra
     * - they they are in the same mass range
     * @param experiment
     * @param peaklists a peaklist for each spectrum
     * @return a list of merged peaks
     */
    ArrayList<ProcessedPeak> mergePeaks(Ms2Experiment experiment, List<ProcessedPeak> peaklists) {
        final ArrayList<ProcessedPeak> mergedPeaks = new ArrayList<ProcessedPeak>(peaklists.size());
        peakMerger.mergePeaks(peaklists, experiment, experiment.getMeasurementProfile().getAllowedMassDeviation(), new Merger() {
            @Override
            public ProcessedPeak merge(List<ProcessedPeak> peaks, int index, double newMz) {
                final ProcessedPeak newPeak = peaks.get(index);
                // sum up global intensities, take maximum of local intensities
                double local=0d, global=0d,relative=0d;
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
                for (int i=0; i < peaks.size(); ++i) originalPeaks[i] = peaks.get(i).getOriginalPeaks().get(0);
                newPeak.setOriginalPeaks(Arrays.asList(originalPeaks));
                mergedPeaks.add(newPeak);
                return newPeak;
            }
        });
        return mergedPeaks;
    }

    ArrayList<ProcessedPeak> normalize(Ms2Experiment experiment) {
        final double parentMass  = experiment.getIonMass();
        final ArrayList<ProcessedPeak> peaklist = new ArrayList<ProcessedPeak>(100);
        final Deviation mergeWindow = experiment.getMeasurementProfile().getAllowedMassDeviation();
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
            for (int i=0; i < s.size(); ++i) {
                // get index of peak in mass-ordered spectrum
                final double mz = sortedByIntensity.getMzAt(i);
                final int index = Spectrums.binarySearch(sortedByMass, mz);
                assert index >= 0;
                if (deletedPeaks.get(index)) continue; // peak is already deleted
                // delete all peaks within the mass range
                for (int j = index-1; j >= 0 && mergeWindow.inErrorWindow(mz, sortedByMass.getMzAt(j)); --j )
                    deletedPeaks.set(j, true);
                for (int j = index+1; j < s.size() && mergeWindow.inErrorWindow(mz, sortedByMass.getMzAt(j)); ++j )
                    deletedPeaks.set(j, true);
            }
            final int offset = peaklist.size();
            // add all remaining peaks to the peaklist
            for (int i=0; i < s.size(); ++i){
                if (!deletedPeaks.get(i)) {
                    final ProcessedPeak propeak = new ProcessedPeak(new MS2Peak(s, sortedByMass.getMzAt(i), sortedByMass.getIntensityAt(i)));
                    propeak.setIon(ion);
                    peaklist.add(propeak);

                }
            }
            // now normalize spectrum. Ignore peaks near to the parent peak
            final double lowerbound = parentMass - 0.1d;
            double scale = 0d;
            for (int i=offset; i < peaklist.size() && peaklist.get(i).getMz() < lowerbound; ++i) {
                scale = Math.max(scale, peaklist.get(i).getIntensity());
            }
            // now set local relative intensities
            for (int i=offset; i < peaklist.size(); ++i) {
                final ProcessedPeak peak = peaklist.get(i);
                peak.setLocalRelativeIntensity(peak.getIntensity()/scale);
            }
            // and adjust global relative intensity
            globalMaxIntensity = Math.max(globalMaxIntensity, scale);
        }
        // now calculate global normalized intensities
        for (ProcessedPeak peak : peaklist) {
            peak.setGlobalRelativeIntensity(peak.getIntensity()/globalMaxIntensity);
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

    Ms2Experiment validate(Ms2Experiment experiment) {
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

    public MeasurementProfile getDefaultProfile() {
        return defaultProfile;
    }

    public void setDefaultProfile(MeasurementProfile defaultProfile) {
        this.defaultProfile = defaultProfile;
    }
}
