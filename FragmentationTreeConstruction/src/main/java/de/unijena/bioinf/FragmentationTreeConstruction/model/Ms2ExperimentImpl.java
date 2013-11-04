package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple mutable implementation of Ms2Experiment
 */
public class Ms2ExperimentImpl implements Ms2Experiment {

    private List<Ms2Spectrum> ms2Spectra;
    private List<Spectrum<Peak>> ms1Spectra;
    private Spectrum<Peak> mergedMs1Spectrum;
    private double ionMass, moleculeNeutralMass;
    private MeasurementProfile measurementProfile;
    private MolecularFormula molecularFormula;
    private Ionization ionization;
    private boolean preprocessed;

    public Ms2ExperimentImpl(Ms2Experiment exp) {
        this.ms2Spectra = new ArrayList<Ms2Spectrum>(safeArrayListCreation(exp.getMs2Spectra()));
        this.ms1Spectra = new ArrayList<Spectrum<Peak>>(safeArrayListCreation(exp.getMs1Spectra()));
        this.mergedMs1Spectrum = exp.getMergedMs1Spectrum();
        this.ionMass = exp.getIonMass();
        this.moleculeNeutralMass = exp.getMoleculeNeutralMass();
        this.measurementProfile = exp.getMeasurementProfile()==null ? null : exp.getMeasurementProfile();
        this.molecularFormula = exp.getMolecularFormula();
        this.ionization = exp.getIonization();
    }

    public Ms2ExperimentImpl() {

    }

    public List<Ms2Spectrum> getMs2Spectra() {
        return ms2Spectra;
    }

    public void setMs2Spectra(List<Ms2Spectrum> ms2Spectra) {
        this.ms2Spectra = ms2Spectra;
    }

    public List<Spectrum<Peak>> getMs1Spectra() {
        return ms1Spectra;
    }

    public void setMs1Spectra(List<Spectrum<Peak>> ms1Spectra) {
        this.ms1Spectra = ms1Spectra;
    }

    public Spectrum<Peak> getMergedMs1Spectrum() {
        return mergedMs1Spectrum;
    }

    public void setMergedMs1Spectrum(Spectrum<Peak> mergedMs1Spectrum) {
        this.mergedMs1Spectrum = mergedMs1Spectrum;
    }

    public double getIonMass() {
        return ionMass;
    }

    public void setIonMass(double ionMass) {
        this.ionMass = ionMass;
    }

    public double getMoleculeNeutralMass() {
        return moleculeNeutralMass;
    }

    public void setMoleculeNeutralMass(double moleculeNeutralMass) {
        this.moleculeNeutralMass = moleculeNeutralMass;
    }

    public MeasurementProfile getMeasurementProfile() {
        return measurementProfile;
    }

    public void setMeasurementProfile(MeasurementProfile measurementProfile) {
        this.measurementProfile = measurementProfile;
    }

    public MolecularFormula getMolecularFormula() {
        return molecularFormula;
    }

    public void setMolecularFormula(MolecularFormula molecularFormula) {
        this.molecularFormula = molecularFormula;
    }

    public Ionization getIonization() {
        return ionization;
    }

    public void setIonization(Ionization ionization) {
        this.ionization = ionization;
    }

    private static <T> ArrayList<T> safeArrayListCreation(List<T> list) {
        if (list==null) return new ArrayList<T>();
        else return new ArrayList<T>(list);
    }

}
