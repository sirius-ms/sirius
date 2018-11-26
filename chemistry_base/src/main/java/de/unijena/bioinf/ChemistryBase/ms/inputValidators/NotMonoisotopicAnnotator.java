package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import java.util.Collections;
import java.util.List;

public class NotMonoisotopicAnnotator implements QualityAnnotator {
    private Deviation findMs1PeakDeviation;
    private List<SpectrumProperty> prerequisites = Collections.singletonList(SpectrumProperty.NoMS1Peak);

    public NotMonoisotopicAnnotator(Deviation findMs1PeakDeviation) {
        this.findMs1PeakDeviation = findMs1PeakDeviation;
    }

    @Override
    public SpectrumProperty getPropertyToAnnotate() {
        return SpectrumProperty.NotMonoisotopicPeak;
    }

    @Override
    public List<SpectrumProperty> getPrerequisites() {
        return prerequisites;
    }

    @Override
    public void prepare(DatasetStatistics statistics) {

    }

    @Override
    public void annotate(Ms2Dataset dataset) {
        for (Ms2Experiment ms2Experiment : dataset) {
            if (CompoundQuality.hasProperty(ms2Experiment, SpectrumProperty.NoMS1Peak)) continue;
            if (isNotMonoisotopicPeak(ms2Experiment)) {
                CompoundQuality.setProperty(ms2Experiment, SpectrumProperty.NotMonoisotopicPeak);
                continue;
            }
        }
    }

    private boolean isNotMonoisotopicPeak(Ms2Experiment experiment) {
        double precursorMass = experiment.getIonMass();
        int mostIntensiveIdx = -1;
        double maxIntensity = -1d;
        int pos = -1;
        for (Spectrum<Peak> spectrum : experiment.getMs1Spectra()) {
            ++pos;
            int idx = Spectrums.mostIntensivePeakWithin(spectrum, precursorMass, findMs1PeakDeviation);
            if (idx<0) continue;
            double intensity = spectrum.getIntensityAt(idx);
            if (intensity>maxIntensity){
                maxIntensity = intensity;
                mostIntensiveIdx = pos;
            }
        }
        if (mostIntensiveIdx<0) throw new RuntimeException("no MS1 precursor peak found.");

        return isNotMonoisotopicPeak(precursorMass, experiment.getMs1Spectra().get(mostIntensiveIdx), experiment.getAnnotationOrDefault(MS1MassDeviation.class), experiment.getPrecursorIonType().getCharge());

    }

    private boolean isNotMonoisotopicPeak(double precursorMass, Spectrum<Peak> ms1, MS1MassDeviation deviation, int charge) {
        //todo which devation to find peak?
        SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum(ms1);
        int idx = Spectrums.mostIntensivePeakWithin(spectrum, precursorMass, findMs1PeakDeviation);
        double realMass = spectrum.getMzAt(idx);
        if (idx<0){
//            ms1.addPeak(precursorMass, experiment.getIonMass());
            throw new RuntimeException("could not find precursor peak");
        }
        Spectrums.filterIsotpePeaks(spectrum, deviation.allowedMassDeviation);
        int idx2 = Spectrums.mostIntensivePeakWithin(spectrum, precursorMass, findMs1PeakDeviation);
        return (idx2 <0 || realMass!=spectrum.getMzAt(idx2));
//        return (Spectrums.search(ms1, precursorMass, profile.getAllowedMassDeviation())<0); //not contained after filtering
    }

}
