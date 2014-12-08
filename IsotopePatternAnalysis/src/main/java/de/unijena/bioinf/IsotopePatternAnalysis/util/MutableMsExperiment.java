package de.unijena.bioinf.IsotopePatternAnalysis.util;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;

import java.util.ArrayList;
import java.util.List;

public class MutableMsExperiment implements MsExperiment {

    private MeasurementProfile profile;
    private Ionization ionization;
    private ArrayList<Spectrum<Peak>> ms1Spectra;
    private Spectrum<Peak> mergedMs1Spectrum;

    public MutableMsExperiment() {
    }

    public MutableMsExperiment(MutableMeasurementProfile profile, Ionization ionization, List<? extends Spectrum<? extends Peak>> ms1Spectra, Spectrum<? extends Peak> mergedMs1Spectrum) {
        this.profile = profile;
        this.ionization = ionization;
        this.ms1Spectra = new ArrayList<Spectrum<Peak>>();
        for (Spectrum<? extends Peak> s : ms1Spectra) this.ms1Spectra.add(new SimpleMutableSpectrum(s));
        this.mergedMs1Spectrum = mergedMs1Spectrum == null ? null : new SimpleMutableSpectrum(mergedMs1Spectrum);
    }

    public MutableMsExperiment(MsExperiment experiment) {
        this(experiment.getMeasurementProfile() == null ? null : new MutableMeasurementProfile(experiment.getMeasurementProfile()), experiment.getIonization(), experiment.getMs1Spectra(), experiment.getMergedMs1Spectrum());
    }

    public MeasurementProfile getMeasurementProfile() {
        return profile;
    }

    public void setMeasurementProfile(MeasurementProfile profile) {
        this.profile = profile;
    }

    public Ionization getIonization() {
        return ionization;
    }

    public void setIonization(Ionization ionization) {
        this.ionization = ionization;
    }

    public ArrayList<Spectrum<Peak>> getMs1Spectra() {
        return ms1Spectra;
    }

    public void setMs1Spectra(ArrayList<Spectrum<Peak>> ms1Spectra) {
        this.ms1Spectra = ms1Spectra;
    }

    public Spectrum<Peak> getMergedMs1Spectrum() {
        return mergedMs1Spectrum;
    }

    public void setMergedMs1Spectrum(Spectrum<Peak> mergedMs1Spectrum) {
        this.mergedMs1Spectrum = mergedMs1Spectrum;
    }
}
