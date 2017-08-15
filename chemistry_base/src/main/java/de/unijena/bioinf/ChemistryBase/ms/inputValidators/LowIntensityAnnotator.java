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
//      if (!isNotBadQuality(experiment)) continue; //todo fast or better statistics?
        if (CompoundQuality.hasProperty(experiment, SpectrumProperty.NoMS1Peak)) return;
        Spectrum<Peak> ms1 = experiment.getMergedMs1Spectrum();
        double highestInCurrentMs1 = Spectrums.getMaximalIntensity(ms1);
        double ionIntensity = ms1.getIntensityAt(Spectrums.mostIntensivePeakWithin(ms1, experiment.getIonMass(), findMs1PeakDeviation));
        if (ionIntensity/highestInCurrentMs1<minRelMs1Intensity) CompoundQuality.setProperty(experiment, SpectrumProperty.LowIntensity);
        else if (ionIntensity<minRelMs1Intensity) CompoundQuality.setProperty(experiment, SpectrumProperty.LowIntensity);
        //todo another way with absolute intensities
//      else if (ionIntensity<10*datasetStatistics.getMedianMs2NoiseIntensity()) setSpectrumProperty(experiment, SpectrumProperty.LowIntensity); //todo ???

    }
}
