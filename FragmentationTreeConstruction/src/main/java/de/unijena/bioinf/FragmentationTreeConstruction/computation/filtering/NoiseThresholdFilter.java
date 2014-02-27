package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
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
        final ArrayList<Ms2Spectrum<? extends Peak>> spectra = new ArrayList<Ms2Spectrum<? extends Peak>>(specs.size());
        final Deviation allowedDev = experiment.getMeasurementProfile().getAllowedMassDeviation();
        final Deviation parentWindow = new Deviation(allowedDev.getPpm(), Math.min(allowedDev.getAbsolute(), 0.1d));
        for (Ms2Spectrum<? extends Peak> spec : specs) {
            final SimpleMutableSpectrum ms = new SimpleMutableSpectrum();
            for (Peak p : spec) if (p.getIntensity() > threshold || parentWindow.inErrorWindow(experiment.getIonMass(), p.getMass()) ) ms.addPeak(p);
            final Ms2SpectrumImpl ms2 = new Ms2SpectrumImpl(ms, spec.getCollisionEnergy(), spec.getPrecursorMz(), spec.getTotalIonCount());
            spectra.add(ms2);
        }
        final Ms2ExperimentImpl exp = new Ms2ExperimentImpl(experiment);
        exp.setMs2Spectra(spectra);
        return exp;
    }
}
