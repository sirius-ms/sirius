package de.unijena.bioinf.FragmentationTreeConstruction.computation;


import com.sun.javafx.geom.Edge;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.PostProcessor;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.Preprocessor;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.GraphBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.SubFormulaGraphBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.InputValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.Warning;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.HighIntensityMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.Merger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.PeakMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.normalizing.NormalizationType;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DecompositionScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.PeakPairScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.PeakScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.legacy.PeakPairScoreList;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.MassDecomposer.Interval;

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
    private List<PeakPairScorer> peakPairScorers;
    private List<PeakScorer> fragmentPeakScorers;
    private GraphBuilder graphBuilder;
    private List<Preprocessor> preprocessors;
    private List<PostProcessor> postProcessors;

    public FragmentationPatternAnalysis() {
        this.inputValidators = new ArrayList<InputValidator>();
        this.validatorWarning = new Warning.Noop();
        this.normalizationType = NormalizationType.GLOBAL;
        this.peakMerger = new HighIntensityMerger();
        this.decomposers = new DecomposerCache();
        this.repairInput = true;
        this.decompositionScorers = new ArrayList<DecompositionScorer<?>>();
        this.preprocessors = new ArrayList<Preprocessor>();
        this.postProcessors = new ArrayList<PostProcessor>();
        this.rootScorers = new ArrayList<DecompositionScorer<?>>();
        this.peakPairScorers = new ArrayList<PeakPairScorer>();
        this.fragmentPeakScorers = new ArrayList<PeakScorer>();
        this.graphBuilder = new SubFormulaGraphBuilder();
    }

    public FragmentationGraph buildGraph(ProcessedInput input, ScoredMolecularFormula candidate) {
        // build Graph
        final FragmentationGraph graph = graphBuilder.buildGraph(input, candidate);
        // score graph
        final Iterator<Loss> edges = graph.lossIterator();
        while (edges.hasNext()) {
            final Loss loss = edges.next();
            final Fragment u = loss.getHead();
            final Fragment v = loss.getTail();
            final double score = v.getDecomposition().getScore();


        }
    }

    public ProcessedInput preprocessing(Ms2Experiment experiment) {
        // use a mutable experiment, such that we can easily modify it. Validate and preprocess input
        Ms2ExperimentImpl input = wrapInput(preProcess(validate(experiment)));
        // normalize all peaks and merge peaks within the same spectrum
        // put peaks from all spectra together in a flatten list
        List<ProcessedPeak> peaks = normalize(input);
        peaks = postProcess(PostProcessor.Stage.AFTER_NORMALIZING, new ProcessedInput(experiment, peaks, null, null)).getMergedPeaks();
        // merge peaks from different spectra
        final List<ProcessedPeak> processedPeaks = mergePeaks(experiment, peaks);
        // and sort the resulting peaklist by mass
        Collections.sort(processedPeaks, new ProcessedPeak.MassComparator());
        // now search the parent peak. If it is not contained in the spectrum: create one!
        // delete all peaks behind the parent, such that the parent is the heaviest peak in the spectrum
        // Now we can access the parent peak by peaklist[peaklist.size-1]
        final double parentmass = experiment.getIonMass();
        final Deviation parentDeviation = experiment.getMeasurementProfile().getExpectedIonMassDeviation();
        for (int i=processedPeaks.size()-1; i >= 0; --i) {
            if (!parentDeviation.inErrorWindow(parentmass, processedPeaks.get(i).getMz())) {
                if (processedPeaks.get(i).getMz() < parentmass) {
                    // parent peak is not contained. Create a synthetic one
                    final ProcessedPeak syntheticParent = new ProcessedPeak();
                    syntheticParent.setMz(parentmass);
                    processedPeaks.add(syntheticParent);
                    break;
                } else processedPeaks.remove(i);
            } else break;
        }
        assert parentDeviation.inErrorWindow(parentmass, processedPeaks.get(processedPeaks.size()-1).getMz()) : "heaviest peak is parent peak";
        // the distance between parent peak and next peak have to be greater than 2*parentDeviation
        final Deviation parentDeviation2 = parentDeviation.multiply(2);
        final ProcessedPeak parentPeak = processedPeaks.get(processedPeaks.size()-1);
        while (processedPeaks.size()>1 && parentDeviation2.inErrorWindow(parentPeak.getMz(), processedPeaks.get(processedPeaks.size()-2).getMz())) {
            processedPeaks.set(processedPeaks.size()-2, parentPeak);
            processedPeaks.remove(processedPeaks.size()-1);
        }
        final List<ProcessedPeak> afterMerging =
                postProcess(PostProcessor.Stage.AFTER_MERGING, new ProcessedInput(experiment, processedPeaks, parentPeak, null)).getMergedPeaks();
        // decompose and score all peaks
        return decomposeAndScore(experiment, afterMerging, parentDeviation, parentPeak);
    }

    public ProcessedInput decomposeAndScore(Ms2Experiment experiment, List<ProcessedPeak> processedPeaks, Deviation parentDeviation, ProcessedPeak parentPeak) {
        // decompose peaks
        final FormulaConstraints constraints = experiment.getMeasurementProfile().getFormulaConstraints();
        final MassToFormulaDecomposer decomposer = decomposers.getDecomposer(constraints.getChemicalAlphabet());
        final Ionization ion = experiment.getIonization();
        final Deviation fragmentDeviation = experiment.getMeasurementProfile().getExpectedFragmentMassDeviation();
        final List<MolecularFormula> pmds = decomposer.decomposeToFormulas(parentPeak.getMass(), parentDeviation, constraints);
        final ArrayList<List<MolecularFormula>> decompositions = new ArrayList<List<MolecularFormula>>(processedPeaks.size());
        for (ProcessedPeak peak : processedPeaks) {
            decompositions.add(decomposer.decomposeToFormulas(ion.subtractFromMass(peak.getMass()), fragmentDeviation, constraints));
        }
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
    protected ArrayList<ProcessedPeak> mergePeaks(Ms2Experiment experiment, List<ProcessedPeak> peaklists) {
        final ArrayList<ProcessedPeak> mergedPeaks = new ArrayList<ProcessedPeak>(peaklists.size());
        peakMerger.mergePeaks(mergedPeaks, experiment, experiment.getMeasurementProfile().getExpectedFragmentMassDeviation(), new Merger() {
            @Override
            public ProcessedPeak merge(List<ProcessedPeak> peaks, int index, double newMz) {
                final ProcessedPeak newPeak = peaks.get(index);
                // sum up global intensities, take maximum of local intensities
                double local=0d, global=0d;
                for (ProcessedPeak p : peaks) {
                    local = Math.max(local, p.getLocalRelativeIntensity());
                    global += p.getGlobalRelativeIntensity();
                }
                newPeak.setMz(newMz);
                newPeak.setLocalRelativeIntensity(local);
                newPeak.setGlobalRelativeIntensity(global);
                final MS2Peak[] originalPeaks = new MS2Peak[peaks.size()];
                for (int i=0; i < peaks.size(); ++i) originalPeaks[i] = peaks.get(i).getOriginalPeaks().get(0);
                newPeak.setOriginalPeaks(Arrays.asList(originalPeaks));
                mergedPeaks.add(newPeak);
                return newPeak;
            }
        });
        return mergedPeaks;
    }

    public ArrayList<ProcessedPeak> normalize(Ms2Experiment experiment) {
        final double parentMass  = experiment.getIonMass();
        final ArrayList<ProcessedPeak> peaklist = new ArrayList<ProcessedPeak>(100);
        final Deviation mergeWindow = experiment.getMeasurementProfile().getExpectedFragmentMassDeviation();
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
                    peaklist.add(new ProcessedPeak(new MS2Peak(s, sortedByMass.getMzAt(i), sortedByMass.getIntensityAt(i))));
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

    public Ms2Experiment preProcess(Ms2Experiment experiment) {
        for (Preprocessor proc : preprocessors) {
            experiment = proc.process(experiment);
        }
        return experiment;
    }

    public ProcessedInput postProcess(PostProcessor.Stage stage, ProcessedInput input) {
        for (PostProcessor proc : postProcessors) {
            if (proc.getStage() == stage) {
                input = proc.process(input);
            }
        }
        return input;
    }

    public Ms2Experiment validate(Ms2Experiment experiment) {
        for (InputValidator validator : inputValidators) {
            experiment = validator.validate(experiment, validatorWarning, repairInput);
        }
        return experiment;
    }

    private Ms2ExperimentImpl wrapInput(Ms2Experiment exp) {
        if (exp instanceof Ms2ExperimentImpl) return (Ms2ExperimentImpl) exp;
        else return new Ms2ExperimentImpl(exp);
    }

}
