package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FewPeaksAnnotator implements QualityAnnotator {
    private final static Logger LOG = LoggerFactory.getLogger(FewPeaksAnnotator.class);
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
        try {
            intensityThreshold = statistics.getMedianMs2NoiseIntensity();
        } catch (IllegalStateException e){
            intensityThreshold = 0;
            LOG.warn("Unknown median noise intensity: No noise peaks found. Setting to 0.");
        }
    }

    /**
     * merges Ms2 and counts number of peaks with intensity &gt;= 2 * median noise intensity.
     * annotates {@link Ms2Experiment} with SpectrumProperty.FewPeaks if it contains less than minNumberOfPeaks
     * @param dataset
     */
    @Override
    public void annotate(Ms2Run dataset) {
        //to few peaks
        for (Ms2Experiment experiment : dataset.getExperiments()) {
            //todo merge Ms2 beforehand?
            if (experiment.getMs2Spectra().size()==0){
                CompoundQuality.setProperty(experiment, SpectrumProperty.FewPeaks);
                continue;
            }
            MS2MassDeviation ms2Deviation = experiment.getAnnotationOrDefault(MS2MassDeviation.class);
            Spectrum<Peak> ms2Spec = getMergedMs2(experiment, ms2Deviation.allowedMassDeviation);
            if (Double.isNaN(dataset.getIsolationWindowWidth()) || dataset.getIsolationWindowWidth()>1){
                SimpleMutableSpectrum mutableSpectrum = new SimpleMutableSpectrum(ms2Spec);
//                Spectrums.filterIsotpePeaks(mutableSpectrum, dataset.getMeasurementProfile().getAllowedMassDeviation());
                //changed to mass difference deviation
                Spectrums.filterIsotpePeaks(mutableSpectrum, ms2Deviation.massDifferenceDeviation);
                ms2Spec = mutableSpectrum;
            }

            int numberOfPeaks = 0;
            for (Peak peak : ms2Spec) {
                //todo what is a peak?
                if (peak.getIntensity()>=2*intensityThreshold) numberOfPeaks++;
            }

            if (numberOfPeaks<minNumberOfPeaks) CompoundQuality.setProperty(experiment, SpectrumProperty.FewPeaks);
        }
    }


    private Spectrum<Peak> getMergedMs2(Ms2Experiment experiment, Deviation deviation){
        if (experiment.getMs2Spectra().size()==1) return experiment.getMs2Spectra().get(0);
        else {
            //don't sum intensities
            //todo only merge between different spectra.
            return Spectrums.mergeSpectra(deviation, false, true, experiment.getMs2Spectra());
        }
    }

    private Spectrum<Peak> getMostIntenseMs2(Ms2Experiment experiment){
        double precursorMass = experiment.getIonMass();
        int mostIntensiveIdx = -1;
        double maxIntensity = -1d;
        int pos = -1;
        for (Spectrum<Peak> spectrum : experiment.getMs1Spectra()) {
            ++pos;
            int idx = Spectrums.mostIntensivePeakWithin(spectrum, precursorMass, new Deviation(100));
            if (idx<0) continue;
            double intensity = spectrum.getIntensityAt(idx);
            if (intensity>maxIntensity){
                maxIntensity = intensity;
                mostIntensiveIdx = pos;
            }
        }
        if (mostIntensiveIdx<0){
            pos = -1;
            for (Spectrum<Peak> spectrum : experiment.getMs2Spectra()) {
                ++pos;
                final int n = spectrum.size();
                double sumIntensity = 0d;
                for (int i = 0; i < n; ++i) {
                    sumIntensity += spectrum.getIntensityAt(i);
                }
                if (sumIntensity>maxIntensity){
                    maxIntensity = sumIntensity;
                    mostIntensiveIdx = pos;
                }
            }
        }
        return experiment.getMs2Spectra().get(mostIntensiveIdx);
    }
}
