package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

import java.util.List;

public class JenaMsExperiment implements Ms2Experiment {

    private List<JenaMs2Spectrum> ms2Spectra;
    private List<JenaMsSpectrum> ms1Spectra;
    private Ionization ionization;
    private double ionMass;
    private int charge;
    private MolecularFormula compoundFormula;
    private String compoundName;

    JenaMsExperiment(String compoundName, MolecularFormula compoundFormula, double ionMass, int charge, Ionization ionization, List<JenaMsSpectrum> ms1Spectra, List<JenaMs2Spectrum> ms2Spectra) {
        this.compoundName = compoundName;
        this.compoundFormula = compoundFormula;
        this.ionMass = ionMass;
        this.charge = charge;
        this.ionization = ionization;
        this.ms1Spectra = ms1Spectra;
        this.ms2Spectra = ms2Spectra;
    }

    public String getCompoundName() {
        return compoundName;
    }

    public int getCharge() {
        return charge;
    }

    @Override
    public List<JenaMs2Spectrum> getMs2Spectra() {
        return ms2Spectra;
    }

    @Override
    public List<JenaMsSpectrum> getMs1Spectra() {
        return ms1Spectra;
    }

    @Override
    public JenaMsSpectrum getMergedMs1Spectrum() {
        return null;
    }

    @Override
    public double getIonMass() {
        return ionMass;
    }

    @Override
    public boolean isPreprocessed() {
        return false;
    }

    @Override
    public MeasurementProfile getMeasurementProfile() {
        return null;
    }

    @Override
    public double getMoleculeNeutralMass() {
        return compoundFormula != null ? compoundFormula.getMass() : Double.NaN;
    }

    @Override
    public MolecularFormula getMolecularFormula() {
        return compoundFormula;
    }

    @Override
    public Ionization getIonization() {
        return ionization;
    }
}
