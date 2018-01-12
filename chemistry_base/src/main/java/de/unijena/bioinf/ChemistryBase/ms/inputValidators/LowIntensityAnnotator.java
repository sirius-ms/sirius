package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import java.util.Collections;
import java.util.List;

public class LowIntensityAnnotator implements QualityAnnotator{
    private DatasetStatistics statistics;
    private List<SpectrumProperty> prerequisite = Collections.singletonList(SpectrumProperty.NoMS1Peak);
    private Deviation findMs1PeakDeviation;
    double minRelMs1Intensity;

    public LowIntensityAnnotator(Deviation findMs1PeakDeviation, double minRelMs1Intensity, double minAbsMs1Intensity) {
        this.findMs1PeakDeviation = findMs1PeakDeviation;
        this.minRelMs1Intensity = minRelMs1Intensity;
    }

    @Override
    public SpectrumProperty getPropertyToAnnotate() {
        return SpectrumProperty.LowIntensity;
    }

    @Override
    public List<SpectrumProperty> getPrerequisites() {
        return prerequisite;
    }

    @Override
    public void prepare(DatasetStatistics statistics) {
        this.statistics = statistics;
    }

    @Override
    public void annotate(Ms2Dataset dataset) {
        for (Ms2Experiment ms2Experiment : dataset.getExperiments()) {
            annotate(ms2Experiment);
        }
    }

    public void annotate(Ms2Experiment experiment) {
        //too low MS1 peak intensity
        double maxMs1Intensity = statistics.getMaxMs1Intensity();
        if (CompoundQuality.hasProperty(experiment, SpectrumProperty.NoMS1Peak)) return;

        boolean isLowIntensity = true;
        for (Spectrum<Peak> spectrum : experiment.getMs1Spectra()) {
            double highestInCurrentMs1 = Spectrums.getMaximalIntensity(spectrum);
            double ionIntensity = spectrum.getIntensityAt(Spectrums.mostIntensivePeakWithin(spectrum, experiment.getIonMass(), findMs1PeakDeviation));
            if (ionIntensity/highestInCurrentMs1>=minRelMs1Intensity) isLowIntensity = false;
//            else if (ionIntensity>=minAbsMs1Intensity) isLowIntensity = false;
            if (!isLowIntensity) break;
        }
        if (isLowIntensity) CompoundQuality.setProperty(experiment, SpectrumProperty.LowIntensity);
    }
}
