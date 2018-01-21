package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import java.util.Collections;
import java.util.List;

public class NoMs1PeakAnnotator implements QualityAnnotator {
    private Deviation findMs1PeakDeviation;

    public NoMs1PeakAnnotator(Deviation findMs1PeakDeviation) {
        this.findMs1PeakDeviation = findMs1PeakDeviation;
    }
    @Override
    public SpectrumProperty getPropertyToAnnotate() {
        return SpectrumProperty.NoMS1Peak;
    }

    @Override
    public List<SpectrumProperty> getPrerequisites() {
        return Collections.emptyList();
    }

    @Override
    public void prepare(DatasetStatistics statistics) {
        //todo use another deviation?
    }

    @Override
    public void annotate(Ms2Dataset dataset) {
        for (Ms2Experiment ms2Experiment : dataset.getExperiments()) {
            annotate(ms2Experiment);
        }
    }

    /**
     * currently annotates {@link Ms2Experiment} with SpectrumProperty.NoMS1Peak if NOT ANY MS1 contains the  precursor peak at ANY intensity.
     * ignores merged MS1 spectrum (this might be a artificial spectrum. E.g. isotopes.)
     * CHANGING DEFINITION OF SpectrumProperty.NoMS1Peak INFLUENCES OTHER QualityAnnotators!
     * TODO do it in a different way? e.g. if not all spectra...
     * @param experiment
     */
    public void annotate(Ms2Experiment experiment) {
//        if (experiment.getMs1Spectra().size()==0) return;
        boolean noMS1 = true;
        for (Spectrum<Peak> spectrum : experiment.getMs1Spectra()) {
            if (Spectrums.binarySearch(spectrum, experiment.getIonMass(), findMs1PeakDeviation)>=0){
                noMS1 = false;
                break;
            }
        }
        if (noMS1) CompoundQuality.setProperty(experiment, SpectrumProperty.NoMS1Peak);
//        Spectrum<Peak> ms1 = experiment.getMergedMs1Spectrum();
//        if (Spectrums.binarySearch(ms1, experiment.getIonMass(), findMs1PeakDeviation)<0){
//            CompoundQuality.setProperty(experiment, SpectrumProperty.NoMS1Peak);
//        }
        //todo is any intensity good enough or should be assume some miniumum intensity (e.g. 0.1%)
        //problem: (at least mzMine2) has relative intensities to precursor (-> has to have a precursor peak anyways?)
    }
}
