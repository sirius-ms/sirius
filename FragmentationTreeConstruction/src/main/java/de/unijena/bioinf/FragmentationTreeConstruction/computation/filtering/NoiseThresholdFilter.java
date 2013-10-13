package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2ExperimentImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2SpectrumImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.ArrayList;
import java.util.List;

public class NoiseThresholdFilter implements PostProcessor, Preprocessor {

    private double threshold;

    public NoiseThresholdFilter() {
        this(0d);
    }

    public NoiseThresholdFilter(double threshold) {
        this.threshold = threshold;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public ProcessedInput process(ProcessedInput input) {
        final List<ProcessedPeak> peaks = input.getMergedPeaks();
        final List<ProcessedPeak> filtered = new ArrayList<ProcessedPeak>(peaks.size());
        final ProcessedPeak parent = input.getParentPeak();
        for (ProcessedPeak p : peaks)
            if (p.getRelativeIntensity() >= threshold || p.isSynthetic() || p == parent)
                filtered.add(p);
        return new ProcessedInput(input.getExperimentInformation(), input.getOriginalInput(), filtered, input.getParentPeak(), input.getParentMassDecompositions(),
                input.getPeakScores(), input.getPeakPairScores());
    }

    @Override
    public Stage getStage() {
        return Stage.AFTER_MERGING;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        threshold = document.getDoubleFromDictionary(dictionary, "threshold");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "threshold", threshold);
    }

    @Override
    public Ms2Experiment process(Ms2Experiment experiment) {
        List<? extends Ms2Spectrum> specs = experiment.getMs2Spectra();
        final ArrayList<Ms2Spectrum> spectra = new ArrayList<Ms2Spectrum>(specs.size());
        for (Ms2Spectrum spec : specs) {
            final SimpleMutableSpectrum ms = new SimpleMutableSpectrum();
            for (Object p : spec) if (((Peak)p).getIntensity() > threshold) ms.addPeak((Peak)p);
            final Ms2SpectrumImpl ms2 = new Ms2SpectrumImpl(ms, spec.getCollisionEnergy(), spec.getPrecursorMz(), spec.getTotalIonCount());
            spectra.add(ms2);
        }
        final Ms2ExperimentImpl exp = new Ms2ExperimentImpl(experiment);
        exp.setMs2Spectra(spectra);
        return exp;
    }
}
