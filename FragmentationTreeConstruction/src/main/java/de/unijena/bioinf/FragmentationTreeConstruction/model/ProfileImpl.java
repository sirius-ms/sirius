package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;

public class ProfileImpl implements MeasurementProfile {

    private Deviation expectedIonMassDeviation, expectedMassDifferenceDeviation, expectedFragmentMassDeviation;
    private FormulaConstraints constraints;
    private double expectedIntensityDeviation;

    public ProfileImpl() {
    }

    public ProfileImpl(Deviation expectedIonMassDeviation, Deviation expectedMassDifferenceDeviation, Deviation expectedFragmentMassDeviation, FormulaConstraints constraints) {
        this.expectedIonMassDeviation = expectedIonMassDeviation;
        this.expectedMassDifferenceDeviation = expectedMassDifferenceDeviation;
        this.expectedFragmentMassDeviation = expectedFragmentMassDeviation;
        this.constraints = constraints;
        this.expectedIntensityDeviation = 1d;  // TODO: add to constructor
    }

    public ProfileImpl(MeasurementProfile profile) {
        this.expectedIonMassDeviation = profile.getExpectedIonMassDeviation();
        this.expectedMassDifferenceDeviation = profile.getExpectedMassDifferenceDeviation();
        this.expectedFragmentMassDeviation = profile.getExpectedFragmentMassDeviation();
        this.constraints = new FormulaConstraints(new ChemicalAlphabet());
        expectedIntensityDeviation = profile.getExpectedIntensityDeviation();
    }

    public Deviation getExpectedIonMassDeviation() {
        return expectedIonMassDeviation;
    }

    public void setExpectedIonMassDeviation(Deviation expectedIonMassDeviation) {
        this.expectedIonMassDeviation = expectedIonMassDeviation;
    }

    public Deviation getExpectedMassDifferenceDeviation() {
        return expectedMassDifferenceDeviation;
    }

    public void setExpectedMassDifferenceDeviation(Deviation expectedMassDifferenceDeviation) {
        this.expectedMassDifferenceDeviation = expectedMassDifferenceDeviation;
    }

    public Deviation getExpectedFragmentMassDeviation() {
        return expectedFragmentMassDeviation;
    }

    public void setFormulaConstraints(FormulaConstraints constraints) {
        this.constraints = constraints;
    }

    @Override
    public FormulaConstraints getFormulaConstraints() {
        return constraints;
    }

    @Override
    public double getExpectedIntensityDeviation() {
        return expectedIntensityDeviation;
    }

    public void setExpectedFragmentMassDeviation(Deviation expectedFragmentMassDeviation) {
        this.expectedFragmentMassDeviation = expectedFragmentMassDeviation;
    }

    public void setConstraints(FormulaConstraints constraints) {
        this.constraints = constraints;
    }

    public void setExpectedIntensityDeviation(double expectedIntensityDeviation) {
        this.expectedIntensityDeviation = expectedIntensityDeviation;
    }
}
