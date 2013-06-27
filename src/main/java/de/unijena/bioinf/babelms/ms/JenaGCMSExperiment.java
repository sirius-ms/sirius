package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.*;

import java.util.List;

public class JenaGCMSExperiment implements Ms2Experiment {

    private List<JenaMsSpectrum> ms1Spectra;
    private Spectrum<Peak> mergedMs1Spectrum;
    private Ionization ionization;
    private double ionMass;
    private MolecularFormula compoundFormula;
    private String compoundName;
    private MeasurementProfile measurementProfile;
    private double collisionEnergy;


    public JenaGCMSExperiment(String compoundName, MolecularFormula compoundFormula, double ionMass, Ionization ionization, List<JenaMsSpectrum> ms1Spectra, double collisionEnergy) {
        this.compoundName = compoundName;
        this.compoundFormula = compoundFormula;
        this.ionMass = ionMass;
        this.ionization = ionization;
        this.ms1Spectra = ms1Spectra;
        this.collisionEnergy = collisionEnergy;
    }

    public String getCompoundName() {
        return compoundName;
    }

    public double getCollisionEnergy() {
        return collisionEnergy;
    }

    @Override
    public List<? extends Ms2Spectrum> getMs2Spectra() {
        return null;
    }

    @Override
    public double getIonMass() {
        return ionMass;
    }

    @Override
    public boolean isPreprocessed() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double getMoleculeNeutralMass() {
        return compoundFormula != null ? compoundFormula.getMass() : Double.NaN;
    }

    @Override
    public MolecularFormula getMolecularFormula() {
        return compoundFormula;
    }

    public void setMeasurementProfile(MeasurementProfile measurementProfile) {
        this.measurementProfile = measurementProfile;
    }

    @Override
    public MeasurementProfile getMeasurementProfile() {
        return measurementProfile;
    }

    @Override
    public Ionization getIonization() {
        return ionization;
    }

    @Override
    public List<? extends Spectrum<Peak>> getMs1Spectra() {
        return ms1Spectra;
    }

    @Override
    public Spectrum<Peak> getMergedMs1Spectrum() {
        return mergedMs1Spectrum;
    }

    public void setMergedMs1Spectrum(Spectrum<Peak> mergedMs1Spectrum) {
        this.mergedMs1Spectrum = mergedMs1Spectrum;
    }



}
