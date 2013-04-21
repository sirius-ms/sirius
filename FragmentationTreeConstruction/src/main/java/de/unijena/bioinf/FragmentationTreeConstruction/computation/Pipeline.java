package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.decomposing.Decomposer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.decomposing.RoundRobinDecomposer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseNoopListener;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoisePeakCallback;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoisePeakFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.PostProcessedNoisePeakFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.GraphBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.SubFormulaGraphBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.ChargedInputFormulaValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.InputValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.HighIntensityMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.Merger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.PeakMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.normalizing.IntensityMaxNormalizer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.normalizing.NormalizationType;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.normalizing.Normalizer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection.Detection;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection.ParentPeakDetector;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.parentPeakDetection.UseInputParentPeak;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.peakprocessor.PeakProcessor;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.legacy.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp.GurobiSolver;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.functional.Function;
import de.unijena.bioinf.functional.list.ListOperations;
import de.unijena.bioinf.functional.list.ParallelListOperations;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static java.lang.Math.log;

/**
 * @author Kai Dührkop
 */
public class Pipeline {

    private static Integer DefaultNumberOfThreads = null;

    private NoisePeakFilter preprocessingFilter;
    private PostProcessedNoisePeakFilter postNormalizingFilter, postProcessingFilter;
    private Decomposer<? extends Object> decomposer;
    private Object initializedDecomposer;
    private ParentPeakDetector parentPeakDetector;
    private PeakMerger peakMerger;
    private Normalizer localNormalizer;
    private Normalizer globalNormalizer;
    private DecompositionScorer<?> decompositionScorer;
    private DecompositionScorer<?> rootScorer;
    private de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.PeakPairScorer PeakPairScorer;
    private LossScorer edgeScorer;
    private NoisePeakCallback noiseListener;
    private GraphBuilder graphBuilder;
    private TreeBuilder treeBuilder;
    private InputValidator validator;
    private NormalizationType normalizationType;
    private ParallelListOperations parallelizer;


    private ArrayList<PeakProcessor> processors;

    public static Pipeline getDefault() {
        final Pipeline pipeline = new Pipeline();
        pipeline.setToDefault();
        return pipeline;
    }

    public Pipeline() {
        parallelizer = new ParallelListOperations();
    }

    public void setToTrained() {
        setToDefault();
        final ArrayList<DecompositionScorer> rootScorers = new ArrayList<DecompositionScorer>();
        final ArrayList<DecompositionScorer> decompositionScorers = new ArrayList<DecompositionScorer>();
        final ArrayList<LossScorer> edgeScorers = new ArrayList<LossScorer>();
        final ArrayList<PeakPairScorer> ms2ClosureScorers = new ArrayList<PeakPairScorer>();
        // A-priori Scorer für die Wurzel: Hetero2Carbon, Hydrogen2Carbon, RDBE
        final NormalDistribution rdbe = new NormalDistribution(6.151312, 4.541604);
        final NormalDistribution het2carb = new NormalDistribution(0.5886335, 0.5550574);
        final NormalDistribution hy2carb = new NormalDistribution(1.435877, 0.4960778);
        rootScorers.add(new Hetero2CarbonVertexScorer(het2carb));
        //rootScorers.add(new Hydrogen2CarbonVertexScorer(hy2carb));
        //rootScorers.add(new RDBEVertexScorer(rdbe));
        // Mass Deviation und Intensity Scorer für alle Fragmente
        rootScorers.add(new MassDeviationVertexScorer(3, 4));
        decompositionScorers.add(new MassDeviationVertexScorer(3, 4));

        final ExponentialDistribution het2carbFrag = new ExponentialDistribution(0.5);
        decompositionScorers.add(new Hetero2CarbonVertexScorer(het2carbFrag, log(0.1)));

        // Kollisionsenergie
        ms2ClosureScorers.add(new CollisionEnergyEdgeScorer(0.1, 0.8));
        // Loss-Size Scoring
        ms2ClosureScorers.add(new MixedLossSizeScorer());
        // Common Losses und Uncommon Losses
        edgeScorers.add(CommonLossEdgeScorer.getDefaultCommonLossScorer(1, 0.005).recombinate(2).merge(
                CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(log(0.001))));
        // penalize pure carbon/nitrogen
        edgeScorers.add(new PureCarbonNitrogenLossScorer(log(0.0001)));
        // radicals
        edgeScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet(log(0.9), log(0.001)));
        // Hetero2Carbon Ratio of Losses
        //edgeScorers.add(new ParentToChildRatioScorer(new Hetero2CarbonVertexScorer(het2carb)));
        // FINISH
        setPeakPairScorer(new PeakPairScoreList(ms2ClosureScorers));
        setEdgeScorer(new EdgeScoreList(edgeScorers));
        setDecompositionScorer(new VertexScoreList(decompositionScorers));
        setRootScorer(new VertexScoreList(rootScorers));
        setNormalizationType(NormalizationType.GLOBAL);

    }

    public void setToDefault() {
        final NormalDistribution rdbe = new NormalDistribution(6.151312, 4.541604);
        final NormalDistribution het2carb = new NormalDistribution(0.5886335, 0.5550574);
        final NormalDistribution hy2carb = new NormalDistribution(1.435877, 0.4960778);
        final int cpus = (getDefaultNumberOfThreads() == null) ? Runtime.getRuntime().availableProcessors() : getDefaultNumberOfThreads().intValue();
        processors = new ArrayList<PeakProcessor>();
        validator = new ChargedInputFormulaValidator();
        preprocessingFilter = null;
        postNormalizingFilter = null;
        postProcessingFilter = null;
        decomposer = RoundRobinDecomposer.withDefaultBoundaries(5);
        initializedDecomposer = null;
        parentPeakDetector = new UseInputParentPeak();
        peakMerger = new HighIntensityMerger();//new KMeansMerger();
        localNormalizer = new IntensityMaxNormalizer(1d);
        globalNormalizer = new IntensityMaxNormalizer(1d);
        final ArrayList<DecompositionScorer> rootScorers = new ArrayList<DecompositionScorer>();
        final ArrayList<DecompositionScorer> decompositionScorers = new ArrayList<DecompositionScorer>();
        final ArrayList<LossScorer> edgeScorers = new ArrayList<LossScorer>();
        final ArrayList<PeakPairScorer> ms2ClosureScorers = new ArrayList<PeakPairScorer>();
        rootScorers.add(new Hetero2CarbonVertexScorer(het2carb));
        //rootScorers.add(new Hydrogen2CarbonVertexScorer(hy2carb));
        //rootScorers.add(new RDBEVertexScorer(rdbe));
        // Mass Deviation und Intensity Scorer für alle Fragmente
        /*
            MassDeviationPenalty: 3 => 99.98% of all peaks are in the mass deviation range of the instrument
            Lambda: 8 => a peak with 75% intensity is allowed to have maximal mass deviation
         */
        rootScorers.add(new MassDeviationVertexScorer(3, 8));
        decompositionScorers.add(new MassDeviationVertexScorer(3, 4));
        // Kollisionsenergie
        ms2ClosureScorers.add(new CollisionEnergyEdgeScorer(0.1, 0.8));
        // Loss-Size Scoring
        ms2ClosureScorers.add(new MixedLossSizeScorer());
        // Common Losses und Uncommon Losses
        edgeScorers.add(CommonLossEdgeScorer.getDefaultCommonLossScorer(1, 0.005).recombinate(2).merge(
                CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(log(0.001))));
        // penalize pure carbon/nitrogen
        edgeScorers.add(new PureCarbonNitrogenLossScorer(log(0.0001)));
        // radicals                                                                                                     2
        edgeScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet(log(0.9), log(0.001)));
        // Hetero2Carbon Ratio of Losses
        //edgeScorers.add(new ParentToChildRatioScorer(new Hetero2CarbonVertexScorer(het2carb)));
        // FINISH
        setPeakPairScorer(new PeakPairScoreList(ms2ClosureScorers));
        setEdgeScorer(new EdgeScoreList(edgeScorers));
        setDecompositionScorer(new VertexScoreList(decompositionScorers));
        setRootScorer(new VertexScoreList(rootScorers));
        setNormalizationType(NormalizationType.GLOBAL);
        noiseListener = new NoiseNoopListener();
        graphBuilder = new SubFormulaGraphBuilder();
        treeBuilder = new GurobiSolver();
        ((GurobiSolver)treeBuilder).setNumberOfCPUs(cpus);
    }

    public void setToOriginalSirius() {
        setToDefault();
        // setze auf originales Scoring:
        final ArrayList<DecompositionScorer> rootScorers = new ArrayList<DecompositionScorer>();
        final ArrayList<DecompositionScorer> decompositionScorers = new ArrayList<DecompositionScorer>();
        final ArrayList<LossScorer> edgeScorers = new ArrayList<LossScorer>();
        final ArrayList<PeakPairScorer> ms2ClosureScorers = new ArrayList<PeakPairScorer>();
        // A-priori Scorer für die Wurzel: Hetero2Carbon, Hydrogen2Carbon, RDBE
        final NormalDistribution rdbe = new NormalDistribution(6.151312, 4.541604);
        final NormalDistribution het2carb = new NormalDistribution(0.5886335, 0.5550574);
        final NormalDistribution hy2carb = new NormalDistribution(1.435877, 0.4960778);
        rootScorers.add(new Hetero2CarbonVertexScorer(het2carb));
        //rootScorers.add(new Hydrogen2CarbonVertexScorer(hy2carb));
        //rootScorers.add(new RDBEVertexScorer(rdbe));
        // Mass Deviation und Intensity Scorer für alle Fragmente
        rootScorers.add(new MassDeviationVertexScorer(3, 4));
        decompositionScorers.add(new MassDeviationVertexScorer(3, 4));
        // Kollisionsenergie
        ms2ClosureScorers.add(new CollisionEnergyEdgeScorer(0.1, 0.8));
        // Loss-Size Scoring
        ms2ClosureScorers.add(new MixedLossSizeScorer());
        // Common Losses und Uncommon Losses
        edgeScorers.add(CommonLossEdgeScorer.getDefaultCommonLossScorer(1, 0.005).recombinate(3).merge(
                CommonLossEdgeScorer.getDefaultUnplausibleLossScorer(log(0.001))));
        // penalize pure carbon/nitrogen
        edgeScorers.add(new PureCarbonNitrogenLossScorer(log(0.0001)));
        // radicals                                                                                                     2
        edgeScorers.add(FreeRadicalEdgeScorer.getRadicalScorerWithDefaultSet(log(0.9), log(0.001)));
        // Hetero2Carbon Ratio of Losses
        edgeScorers.add(new ParentToChildRatioScorer(new Hetero2CarbonVertexScorer(het2carb)));
        // FINISH
        setPeakPairScorer(new PeakPairScoreList(ms2ClosureScorers));
        setEdgeScorer(new EdgeScoreList(edgeScorers));
        setDecompositionScorer(new VertexScoreList(decompositionScorers));
        setRootScorer(new VertexScoreList(rootScorers));
        setNormalizationType(NormalizationType.GLOBAL);
    }

    public static Integer getDefaultNumberOfThreads() {
        return DefaultNumberOfThreads;
    }

    public static void setDefaultNumberOfThreads(Integer defaultNumberOfThreads) {
        DefaultNumberOfThreads = defaultNumberOfThreads;
    }

    public NormalizationType getNormalizationType() {
        return normalizationType;
    }

    public void setNormalizationType(NormalizationType normalizationType) {
        this.normalizationType = normalizationType;
    }

    public ArrayList<PeakProcessor> getProcessors() {
        return processors;
    }

    public void setProcessors(ArrayList<PeakProcessor> processors) {
        this.processors = processors;
    }

    @SuppressWarnings("unchecked")
	public ProcessedInput preprocessPeaks(final MSInput originalInput, final MSExperimentInformation information) {
        final MSInput input = validator.validate(originalInput, true);
        if (initializedDecomposer == null || !((Decomposer<Object>)decomposer).alphabetStillValid(initializedDecomposer, information.getAlphabet())) {
            initializedDecomposer = decomposer.initialize(information.getAlphabet(), information);
        }
        final NoisePeakCallback listener = (noiseListener == null) ? new NoiseNoopListener() : noiseListener;
        // remove noise
        final List<ArrayList<MS2Peak>> peaksPerSpectrum;
        {
            final List<Ms2SpectrumImpl> spectra = input.getMs2Spectra();
            peaksPerSpectrum = ListOperations.singleton().map(spectra, new Function<Ms2SpectrumImpl, ArrayList<MS2Peak>>() {
                @Override
                public ArrayList<MS2Peak> apply(Ms2SpectrumImpl arg) {
                    return new ArrayList<MS2Peak>(arg.getPeaks());
                }
            });
            // remove noise
            if (preprocessingFilter != null) {
                for (ArrayList<MS2Peak> peaks : peaksPerSpectrum) {
                    peaks.removeAll(preprocessingFilter.filter(input, information, peaks, listener));
                }
            }
        }
        // normalize
        final ArrayList<ProcessedPeak> processedPeaks = normalize(input, information, peaksPerSpectrum);
        // TODO: detect parent peak before merging, otherwise the real parent peak could be lost
        for (PeakProcessor p : processors) {
            if (p.getStage() == PeakProcessor.Stage.BEFORE_MERGING) {
                p.process(processedPeaks, input, information, (Decomposer<Object>)decomposer, initializedDecomposer);
            }
        }
        // merge
        final ArrayList<ProcessedPeak> mergedPeaks = mergePeaks(input, information, listener, processedPeaks);
        for (PeakProcessor p : processors) {
            if (p.getStage() == PeakProcessor.Stage.AFTER_MERGING) {
                p.process(mergedPeaks, input, information, (Decomposer<Object>)decomposer, initializedDecomposer);
            }
        }
        // detect or create parent peak
        final ProcessedPeak parentPeak = detectOrCreateParentPeak(input, information, listener, mergedPeaks);
        // decompose and score formulas
        decomposeAndScoreFragments(input, information, mergedPeaks, parentPeak);
        // sort pmds
        {
            final List<ScoredMolecularFormula> pmds = new ArrayList<ScoredMolecularFormula>(parentPeak.getDecompositions());
            Collections.sort(pmds, Collections.reverseOrder());
            int noninfScore = pmds.size()-1;
            while (noninfScore >= 0 && Double.isInfinite(pmds.get(noninfScore).getScore())) --noninfScore;
            parentPeak.setDecompositions(
                    noninfScore < 0 ? new ArrayList<ScoredMolecularFormula>() : new ArrayList<ScoredMolecularFormula>(pmds.subList(0, noninfScore+1))
            );
        }

        return new ProcessedInput(information, input, mergedPeaks, mergedPeaks.get(parentPeak.getIndex()), parentPeak.getDecompositions());
    }

    private ProcessedPeak detectOrCreateParentPeak(MSInput input, MSExperimentInformation information, NoisePeakCallback listener, ArrayList<ProcessedPeak> mergedPeaks) {
        // parent peak detection
        final Detection parentPeakDetection =
                parentPeakDetector.detectParentPeak(new ProcessedInput(information, input, mergedPeaks, null, null), mergedPeaks);
        if (parentPeakDetection == null) {
            throw new RuntimeException("Can't find parent peak for input data");
        }
        final ProcessedPeak parentPeak = (parentPeakDetection.isSynthetic()) ? new ProcessedPeak(parentPeakDetection.getParentPeak())
                                                                                 : (ProcessedPeak)parentPeakDetection.getParentPeak();
        // final filtering
        if (postProcessingFilter != null) {
            final int sizeBefore = mergedPeaks.size();
            final List<ProcessedPeak> toDelete = postProcessingFilter.filter(
                    new ProcessedInput(information, input, mergedPeaks, parentPeak, null), information, mergedPeaks, listener);
            if (!parentPeakDetection.isSynthetic()) toDelete.remove(parentPeak);
            mergedPeaks.removeAll(toDelete);
            assert sizeBefore - toDelete.size() == mergedPeaks.size();
        }
        if (parentPeakDetection.isSynthetic()) {
            assert parentPeakDetection.getParentPeak().getOriginalPeaks().isEmpty();
            parentPeak.setIndex(mergedPeaks.size());
            mergedPeaks.add(parentPeak);
        } else {
            assert !parentPeakDetection.getParentPeak().getOriginalPeaks().isEmpty();
        }
        // set intensity to 100%
        parentPeak.setRelativeIntensity(1);
        parentPeak.setGlobalRelativeIntensity(1);
        assert peaksDontOverlapAfterMerging(information, mergedPeaks);
        int k=0;
        for (ProcessedPeak p : mergedPeaks) {
            p.setIndex(k++);
        }
        return parentPeak;
    }

    private void decomposeAndScoreFragments(MSInput input, MSExperimentInformation information, ArrayList<ProcessedPeak> mergedPeaks, final ProcessedPeak parentPeak) {
        final ProcessedInput pinputMerged = new ProcessedInput(information, input, mergedPeaks, null, null);
        final Object prepareDecompositionScoring = decompositionScorer.prepare(pinputMerged);
        final Object rootDecompositionScoring = rootScorer.prepare(pinputMerged);
        // decompose and score decompositions
        final List<List<ScoredMolecularFormula>> decompositions = parallelizer.mapFixed(mergedPeaks,
                new Function<ProcessedPeak, List<ScoredMolecularFormula>>() {
                    @SuppressWarnings("unchecked")
					@Override
                    public List<ScoredMolecularFormula> apply(ProcessedPeak arg) {
                        final DecompositionScorer<? super Object> scorer = (DecompositionScorer<? super Object>) ((arg == parentPeak) ? rootScorer : decompositionScorer);
                        final List<MolecularFormula> formulas =
                                ((Decomposer<Object>) decomposer).decompose(initializedDecomposer, arg.getUnmodifiedMass(), pinputMerged.getExperimentInformation());
                        // TODO: improve performance
                        if (arg == parentPeak && pinputMerged.getExperimentInformation().getParentPeakMassError() != pinputMerged.getExperimentInformation().getMassError())  {
                            final Iterator<MolecularFormula> iter = formulas.iterator();
                            final Deviation dev = pinputMerged.getExperimentInformation().getParentPeakMassError();
                            while (iter.hasNext())
                                if (!dev.inErrorWindow(iter.next().getMass(), arg.getUnmodifiedMass()))
                                    iter.remove();
                        }
                        final List<ScoredMolecularFormula> decompositions = new ArrayList<ScoredMolecularFormula>(formulas.size());
                        for (MolecularFormula formula : formulas) {
                            final double score;
                            if (arg == parentPeak) {
                                if (parentPeak.getOriginalPeaks().isEmpty()) score = 0; else score = scorer.score(formula, arg, pinputMerged, rootDecompositionScoring);
                            } else {
                                score = scorer.score(formula, arg, pinputMerged, prepareDecompositionScoring);
                            }
                            decompositions.add(new ScoredMolecularFormula(formula, score));
                        }
                        return decompositions;
                    }
                }, 10);
        for (int i=0; i < decompositions.size(); ++i) {
            (mergedPeaks.get(i)).setDecompositions(decompositions.get(i));
        }
    }

    public int getNumberOfParallelTasks() {
        return parallelizer.getNumberOfParallelTasks();
    }

    public void setNumberOfParallelTasks(int numberOfParallelTasks) {
        this.parallelizer.setNumberOfParallelTasks(numberOfParallelTasks);
    }

    public ArrayList<ProcessedPeak> mergePeaks(MSInput input, MSExperimentInformation information, NoisePeakCallback listener, ArrayList<ProcessedPeak> processedPeaks) {
        final ProcessedInput pinput = new ProcessedInput(information, input, Collections.<ProcessedPeak>emptyList(), null, null);
        // post normalizing filter
        if (postNormalizingFilter != null) {
            processedPeaks.removeAll(postNormalizingFilter.filter(pinput, information, processedPeaks, listener));
        }
        // merge
        final ArrayList<ProcessedPeak> mergedPeaks= new ArrayList<ProcessedPeak>(processedPeaks.size());
        peakMerger.mergePeaks(processedPeaks, pinput, new Merger() {
            @Override
            public void merge(List<ProcessedPeak> merged, int mainIndex, double mz, double newRelativeGlobalIntensity) {
                final List<MS2Peak> originals = ListOperations.singleton().map(merged, new Function<ProcessedPeak, MS2Peak>() {
                    @Override
                    public MS2Peak apply(ProcessedPeak arg) {
                        assert arg.getOriginalPeaks().size() == 1;
                        return arg.getOriginalPeaks().get(0);
                    }
                });
                assert peaksAreFromDifferentSpectra(originals);
                final ProcessedPeak pp = new ProcessedPeak(merged.get(mainIndex));
                pp.setLocalRelativeIntensity(0d);
                pp.setIndex(mergedPeaks.size());
                pp.setGlobalRelativeIntensity(0d);
                for (ProcessedPeak p : merged) {
                    pp.setLocalRelativeIntensity(pp.getRelativeIntensity() + p.getRelativeIntensity());
                    pp.setGlobalRelativeIntensity(pp.getGlobalRelativeIntensity() + p.getGlobalRelativeIntensity());
                    pp.setMaxGlobalIntensity(Math.max(pp.getGlobalRelativeIntensity(), p.getGlobalRelativeIntensity()));
                }
                pp.setMz(mz);
                pp.setOriginalPeaks(originals);
                pp.setRelativeIntensity(normalizationType == NormalizationType.LOCAL ? pp.getLocalRelativeIntensity() : pp.getGlobalRelativeIntensity());
                mergedPeaks.add(pp);
            }
        });
        assert peaksDontOverlapAfterMerging(information, mergedPeaks);
        return mergedPeaks;
    }

    private static boolean peaksAreFromDifferentSpectra(List<MS2Peak> originals) {
        for (int i=0; i < originals.size(); ++i) {
            for (int j=i+1; j < originals.size(); ++j) {
                if (originals.get(i).getSpectrum().equals(originals.get(j).getSpectrum())) return false;
            }
        }
        return true;
    }

    private boolean peaksDontOverlapAfterMerging(MSExperimentInformation information, ArrayList<ProcessedPeak> mergedPeaks) {
		final Deviation dev = information.getMassError().multiply(2);
    	for (ProcessedPeak p : mergedPeaks) {
			for (ProcessedPeak q : mergedPeaks) {
				if (p != q && dev.inErrorWindow(p.getMz(), q.getMz())) return false;
			}
		}
    	return true;
	}

	public ArrayList<ProcessedPeak> normalize(MSInput input, MSExperimentInformation information, List<ArrayList<MS2Peak>> peaksPerSpectrum) {
        // TODO: remove normalizers. Use Spectrums.normalize instead!
        ArrayList<MS2Peak> allPeaks = new ArrayList<MS2Peak>();
        for (ArrayList<MS2Peak> peaks : peaksPerSpectrum) allPeaks.addAll(peaks);
        final ArrayList<ProcessedPeak> processedPeaks = new ArrayList<ProcessedPeak>(allPeaks.size());
        final double[] globals = globalNormalizer.normalize(input, information, allPeaks);
        int k=0;
        for (ArrayList<MS2Peak> peaks : peaksPerSpectrum) {
            final double[] locals = localNormalizer.normalize(input, information, peaks);
            for (int i=0; i < locals.length; ++i) {
                final MS2Peak p = peaks.get(i);
                final ProcessedPeak pp = new ProcessedPeak(p);
                pp.setIndex(k);
                pp.setLocalRelativeIntensity(locals[i]);
                pp.setGlobalRelativeIntensity(globals[k]);
                pp.setIon(input.getStandardIon()); // TODO: We "do not know" the ion in practice ;)
                pp.setRelativeIntensity(normalizationType == NormalizationType.LOCAL ? locals[i] : globals[k]);
                processedPeaks.add(pp);
                ++k;
            }
        }
        return processedPeaks;
    }

    public FragmentationGraph buildGraph(ProcessedInput input, ScoredMolecularFormula pmd) {
        final FragmentationGraph graph = graphBuilder.buildGraph(input, pmd);
        // score vertices and edges
        final List<ProcessedPeak> graphPeaks = graph.getPeaks();
        final double[][] ms2Scores = new double[graphPeaks.size()][graphPeaks.size()];
        final Future<?> task = parallelizer.runInBackground(new Runnable() {
            @Override
            public void run() {
                PeakPairScorer.score(graphPeaks, graph.getProcessedInput(), ms2Scores);
            }
        });
        final Object precomputed = edgeScorer.prepare(input, graph);
        final ProcessedInput finput = input;
        final int rootIndex = graph.getRoot().getIndex();
        graph.setRootScore(pmd.getScore());
        final List<double[]> list = parallelizer.mapFixed(graph.getFragments(), new Function<GraphFragment, double[]>() {
            @Override
            public double[] apply(GraphFragment arg) {
                if (arg.getIndex() == rootIndex) return null;
                final double[] output = new double[arg.getIncomingEdges().size()];
                final double vertexScore = arg.getDecomposition().getScore();
                int k=0;
                for (Loss l : arg.getIncomingEdges()) {
                    output[k++] = vertexScore + edgeScorer.score(l, finput, precomputed);
                }
                return output;
            }
        }, 100);
        try {
            task.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        for (int i=0; i < list.size(); ++i) {
            final GraphFragment fragment = graph.getFragment(i);
            if (fragment.getIndex() == rootIndex) continue;
            final double[] scores = list.get(i);
            final List<Loss> edges = fragment.getIncomingEdges();
            assert edges.size() == scores.length;
            for (int j=0; j < edges.size(); ++j) {
                final Loss l = edges.get(j);
                final double ms2Score = ms2Scores[l.getHead().getPeak().getIndex()][fragment.getPeak().getIndex()];
                if (Double.isNaN(scores[j]) || Double.isNaN(ms2Score))
                    throw new RuntimeException("Illegal NaN score for edge " + l);
                l.setWeight(scores[j] + ms2Score);
            }
        }
        graph.prepareForTreeComputation();
        ProcessedInput pinput = new ProcessedInput(input.getExperimentInformation(), input.getOriginalInput(),
                graph.getPeaks(), graph.getRoot().getPeak(), input.getParentMassDecompositions());
        graph.setProcessedInput(pinput);
        return graph;
    }

    public FragmentationTree computeTree(FragmentationGraph graph) {
        return computeTree(graph, 0d);
    }

    public FragmentationTree computeTree(FragmentationGraph graph, double lowerbound) {
        return treeBuilder.buildTree(graph.getProcessedInput(), graph, lowerbound);
    }

    public List<FragmentationTree> computeTrees(ProcessedInput input, int maxNumber) {
        final TreeSet<FragmentationTree> set = new TreeSet<FragmentationTree>();
        int k=0;
        double lowerBound = 0;
        for (ScoredMolecularFormula pmd : input.getParentMassDecompositions()) {
            final FragmentationGraph graph = buildGraph(input, pmd);
            //if (lowerBound > 0 && graph.maximalScore() <= lowerBound) continue;
            final FragmentationTree tree = computeTree(graph, lowerBound);
            if (tree != null && tree.getScore() > lowerBound) {
                assert !Double.isNaN(tree.getScore()) && !Double.isInfinite(tree.getScore());
                set.add(tree);
                if (set.size() > maxNumber) {
                    set.pollFirst();
                    lowerBound = set.first().getScore();
                }
            }
            ++k;
        }
        return new ArrayList<FragmentationTree>(set.descendingSet());
    }

    public List<FragmentationTree> computeTrees(MSInput input, MSExperimentInformation info, int maxNumber) {
        return computeTrees(preprocessPeaks(input, info), maxNumber);
    }

    public NoisePeakFilter getPreprocessingFilter() {
        return preprocessingFilter;
    }

    public void setPreprocessingFilter(NoisePeakFilter preprocessingFilter) {
        this.preprocessingFilter = preprocessingFilter;
    }

    public PostProcessedNoisePeakFilter getPostNormalizingFilter() {
        return postNormalizingFilter;
    }

    public void setPostNormalizingFilter(PostProcessedNoisePeakFilter postNormalizingFilter) {
        this.postNormalizingFilter = postNormalizingFilter;
    }

    public PostProcessedNoisePeakFilter getPostProcessingFilter() {
        return postProcessingFilter;
    }

    public void setPostProcessingFilter(PostProcessedNoisePeakFilter postProcessingFilter) {
        this.postProcessingFilter = postProcessingFilter;
    }

    public Decomposer<? extends Object> getDecomposer() {
        return decomposer;
    }

    public void setDecomposer(Decomposer<? extends Object> decomposer) {
        this.decomposer = decomposer;
        this.initializedDecomposer = null;
    }

    public Object getInitializedDecomposer() {
        return initializedDecomposer;
    }

    public void setInitializedDecomposer(Object initializedDecomposer) {
        this.initializedDecomposer = initializedDecomposer;
    }

    public ParentPeakDetector getParentPeakDetector() {
        return parentPeakDetector;
    }

    public void setParentPeakDetector(ParentPeakDetector parentPeakDetector) {
        this.parentPeakDetector = parentPeakDetector;
    }

    public PeakMerger getPeakMerger() {
        return peakMerger;
    }

    public void setPeakMerger(PeakMerger peakMerger) {
        this.peakMerger = peakMerger;
    }

    public Normalizer getLocalNormalizer() {
        return localNormalizer;
    }

    public void setLocalNormalizer(Normalizer localNormalizer) {
        this.localNormalizer = localNormalizer;
    }

    public Normalizer getGlobalNormalizer() {
        return globalNormalizer;
    }

    public void setGlobalNormalizer(Normalizer globalNormalizer) {
        this.globalNormalizer = globalNormalizer;
    }

    public DecompositionScorer<?> getDecompositionScorer() {
        return decompositionScorer;
    }

    public void setDecompositionScorer(DecompositionScorer<?> decompositionScorer) {
        this.decompositionScorer = decompositionScorer;
    }

    public DecompositionScorer<?> getRootScorer() {
        return rootScorer;
    }

    public void setRootScorer(DecompositionScorer<?> rootScorer) {
        this.rootScorer = rootScorer;
    }

    public LossScorer getEdgeScorer() {
        return edgeScorer;
    }

    public void setEdgeScorer(LossScorer edgeScorer) {
        this.edgeScorer = edgeScorer;
    }

    public NoisePeakCallback getNoiseListener() {
        return noiseListener;
    }

    public void setNoiseListener(NoisePeakCallback noiseListener) {
        this.noiseListener = noiseListener;
    }

    public GraphBuilder getGraphBuilder() {
        return graphBuilder;
    }

    public void setGraphBuilder(GraphBuilder graphBuilder) {
        this.graphBuilder = graphBuilder;
    }

    public TreeBuilder getTreeBuilder() {
        return treeBuilder;
    }

    public void setTreeBuilder(TreeBuilder treeBuilder) {
        this.treeBuilder = treeBuilder;
    }

    public PeakPairScorer getPeakPairScorer() {
        return PeakPairScorer;
    }

    public void setPeakPairScorer(PeakPairScorer peakPairScorer) {
        this.PeakPairScorer = peakPairScorer;
    }
}
