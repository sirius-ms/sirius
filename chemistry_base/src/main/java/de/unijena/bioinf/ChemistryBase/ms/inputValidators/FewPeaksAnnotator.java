package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import java.util.List;

public class FewPeaksAnnotator implements QualityAnnotator {
    private DatasetStatistics statistics;
    private double intensityThreshold;
    private double minNumberOfPeaks;

    public FewPeaksAnnotator(double minNumberOfPeaks) {
        this.minNumberOfPeaks = minNumberOfPeaks;
    }

    @Override
    public SpectrumProperty getPropertyToAnnotate() {
        return SpectrumProperty.FewPeaks;
    }

    @Override
    public List<SpectrumProperty> getPrerequisites() {
        return null;
    }

    @Override
    public void prepare(DatasetStatistics statistics) {
        this.statistics = statistics;
        //todo better noise model
        intensityThreshold = statistics.getMedianMs2NoiseIntensity();
    }

    @Override
    public void annotate(Ms2Dataset dataset) {
        //to few peaks
        for (Ms2Experiment experiment : dataset.getExperiments()) {
            //todo merge Ms2 beforehand?
            Spectrum<Peak> ms2Spec = getMergedMs2(experiment, dataset.getMeasurementProfile().getAllowedMassDeviation());
            if (Double.isNaN(dataset.getIsolationWindowWidth()) || dataset.getIsolationWindowWidth()>1){
                SimpleMutableSpectrum mutableSpectrum = new SimpleMutableSpectrum(ms2Spec);
                Spectrums.filterIsotpePeaks(mutableSpectrum, dataset.getMeasurementProfile().getAllowedMassDeviation());
                ms2Spec = mutableSpectrum;
            }

            int numberOfPeaks = 0;
            for (Peak peak : ms2Spec) {
//                if (peak.getIntensity()>2*datasetStatistics.getMedianMs2NoiseIntensity()) numberOfPeaks++;
                //todo what is a peak?
                if (peak.getIntensity()>intensityThreshold) numberOfPeaks++;
            }

            if (numberOfPeaks<minNumberOfPeaks) CompoundQuality.setProperty(experiment, SpectrumProperty.FewPeaks);
        }
    }


    private Spectrum<Peak> getMergedMs2(Ms2Experiment experiment, Deviation deviation){
        if (experiment.getMs2Spectra().size()==1) return experiment.getMs2Spectra().get(0);
        else {
            //todo problem: this produces summed intensities which is not comparable to medianNoise
            //probably merge not summing -> at least one over noise
            //todo only merge between different spectra.
            return Spectrums.mergeSpectra(deviation, true, true, experiment.getMs2Spectra());
        }
    }
}
