package de.unijena.bioinf.FragmentationTreeConstruction.computation;


import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.InputValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.Warning;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.HighIntensityMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.Merger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.PeakMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.normalizing.NormalizationType;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2ExperimentImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class FragmentationPatternAnalysis {

    private List<InputValidator> inputValidators;
    private Warning validatorWarning;
    private boolean repairInput;
    private NormalizationType normalizationType;
    private PeakMerger peakMerger;

    public FragmentationPatternAnalysis() {
        this.inputValidators = new ArrayList<InputValidator>();
        this.validatorWarning = new Warning.Noop();
        this.normalizationType = NormalizationType.GLOBAL;
        this.peakMerger = new HighIntensityMerger();
        this.decomposer = null;
        this.repairInput = true;
    }

    public ProcessedInput preprocessing(Ms2Experiment experiment) {
        // use a mutable experiment, such that we can easily modify it
        Ms2ExperimentImpl input = wrapInput(validate(experiment));
        final ArrayList<ProcessedPeak> peaks = normalize(input);
        final List<ProcessedPeak> processedPeaks = mergePeaks(experiment, peaks);
        // decompose peaks
        for (ProcessedPeak peak : processedPeaks) {

        }

        // important: for each two peaks which are within 2*massrange:
        //  => make decomposition list disjoint

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
    protected ArrayList<ProcessedPeak> mergePeaks(Ms2Experiment experiment, ArrayList<ProcessedPeak> peaklists) {
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

    protected initializeDecomposer(Ms2Experiment experiment) {
        if (decomposer != null) {
            final MeasurementProfile profile = experiment.getMeasurementProfile();
            if (profile.getExpectedFragmentMassDeviation().getPrecision() != decomposer.get)
        }
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

    protected static class CachedDecomposer {
        private Deviation usedDeviation;
        private ChemicalAlphabet
    }


}
