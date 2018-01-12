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
            if (isNotMonoisotopicPeak(ms2Experiment, dataset.getMeasurementProfile())){
                CompoundQuality.setProperty(ms2Experiment, SpectrumProperty.NotMonoisotopicPeak);
                continue;
            }
        }
    }

    private boolean isNotMonoisotopicPeak(Ms2Experiment experiment, MeasurementProfile profile) {
        final double precursorMass = experiment.getIonMass();

        MutableSpectrum<Peak> merged = new SimpleMutableSpectrum(experiment.getMergedMs1Spectrum());
        //todo which devation to finde peak?
        int idx = Spectrums.mostIntensivePeakWithin(merged, precursorMass, findMs1PeakDeviation);
        double realMass = merged.getMzAt(idx);
        if (idx<0){
//            merged.addPeak(precursorMass, experiment.getIonMass());
            throw new RuntimeException("could not find precursor peak");
        }
        Spectrums.filterIsotpePeaks(merged, profile.getAllowedMassDeviation());
        System.out.println("new");
        int idx2 = Spectrums.mostIntensivePeakWithin(merged, precursorMass, findMs1PeakDeviation);
        return (idx2 <0 || realMass!=merged.getMzAt(idx2));
//        return (Spectrums.search(merged, precursorMass, profile.getAllowedMassDeviation())<0); //not contained after filtering
    }

}
