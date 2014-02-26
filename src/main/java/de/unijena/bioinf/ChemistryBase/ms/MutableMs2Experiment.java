package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.utils.ChargedPeak;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Marcus
 * Date: 16.08.13
 * Time: 16:08
 * To change this template use File | Settings | File Templates.
 */
public class MutableMs2Experiment implements Ms2Experiment {
    private MolecularFormula compoundFormula;
    private double ionMass;
    private Ionization ionization;
    private List<? extends Spectrum<Peak>> ms1Spectra;
    private List<? extends Ms2Spectrum<? extends Peak>> ms2Spectra;
    private MeasurementProfile measurementProfile;
    private Spectrum<Peak> mergedMs1Spectrum;

    public MutableMs2Experiment(){
    }

    public MutableMs2Experiment(Ms2Experiment experiment){
        this(experiment.getMolecularFormula(), experiment.getIonMass(), experiment.getIonization(), experiment.getMs1Spectra(), experiment.getMs2Spectra());
    }

    public MutableMs2Experiment(MolecularFormula compoundFormula, double ionMass, Ionization ionization, List<? extends Spectrum<Peak>> ms1Spectra, List<? extends Ms2Spectrum<? extends Peak>> ms2Spectra){
        this.compoundFormula = compoundFormula;
        this.ionMass = ionMass;
        this.ionization = ionization;
        this.ms1Spectra = ms1Spectra;
        this.ms2Spectra = ms2Spectra;
    }


    @Override
    public List<? extends Ms2Spectrum<? extends Peak>> getMs2Spectra() {
        return ms2Spectra;
    }

    @Override
    public double getIonMass() {
        return ionMass;
    }

    @Override
    public double getMoleculeNeutralMass() {
        //todo also ((ionizaiton!=null && ionMass>0) ? ionization.subtractFrom(....
        return (compoundFormula!=null ? compoundFormula.getMass() : Double.NaN);
    }

    @Override
    public MolecularFormula getMolecularFormula() {
        return compoundFormula;
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

    public void setMolecularFormula(MolecularFormula compoundFormula) {
        this.compoundFormula = compoundFormula;
    }

    public void setIonMass(double ionMass) {
        this.ionMass = ionMass;
    }

    public void setIonization(Ionization ionization) {
        this.ionization = ionization;
    }

    public void setMs1Spectra(List<? extends Spectrum<Peak>> ms1Spectra) {
        this.ms1Spectra = ms1Spectra;
    }

    public void setMs2Spectra(List<? extends Ms2Spectrum<Peak>> ms2Spectra) {
        this.ms2Spectra = ms2Spectra;
    }

    public void setMeasurementProfile(MeasurementProfile measurementProfile) {
        this.measurementProfile = measurementProfile;
    }

    public void setMergedMs1Spectrum(Spectrum<Peak> mergedMs1Spectrum) {
        this.mergedMs1Spectrum = mergedMs1Spectrum;
    }
}
