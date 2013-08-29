package de.unijena.bioinf.GCMSAnalysis.isotopes;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Marcus
 * Date: 24.07.13
 * Time: 12:46
 * To change this template use File | Settings | File Templates.
 */
class IsotopeExperiment implements Ms2Experiment{
    private MutableMeasurementProfile measurementProfile;
    private Ionization ionization;
    public IsotopeExperiment(){
        this.measurementProfile = new MutableMeasurementProfile();
        measurementProfile.setAllowedMassDeviation(new Deviation(7, 5e-2)); //todo welche abweichugn`?
        measurementProfile.setStandardMs1MassDeviation(new Deviation(7, 5e-3));
        measurementProfile.setStandardMassDifferenceDeviation(new Deviation(7, 5e-3));
        measurementProfile.setIntensityDeviation(10);


        PeriodicTable periodicTable =  PeriodicTable.getInstance();
        //introduce new Elements
        List<Element> usedElements = new ArrayList<Element>(Arrays.asList(periodicTable.getAllByName("C", "H", "N", "O", "P", "S")));
        ChemicalAlphabet simpleAlphabet = new ChemicalAlphabet(usedElements.toArray(new Element[0]));

        measurementProfile.setFormulaConstraints(new FormulaConstraints(simpleAlphabet));
        this.ionization = new ElectronIonization();
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T extends Spectrum<Peak>> T getMergedMs1Spectrum() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<? extends Ms2Spectrum> getMs2Spectra() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double getIonMass() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double getMoleculeNeutralMass() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MolecularFormula getMolecularFormula() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
