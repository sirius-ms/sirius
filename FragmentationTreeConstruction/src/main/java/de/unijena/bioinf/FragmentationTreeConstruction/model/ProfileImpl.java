package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;

public class ProfileImpl implements MeasurementProfile {

    private Deviation expectedIonMassDeviation, expectedMassDifferenceDeviation, expectedFragmentMassDeviation;
    private ChemicalAlphabet chemicalAlphabet;

    public ProfileImpl() {

    }

    public ProfileImpl(MeasurementProfile profile) {
        this.expectedIonMassDeviation = profile.getExpectedIonMassDeviation();
        this.expectedMassDifferenceDeviation = profile.getExpectedMassDifferenceDeviation();
        this.expectedFragmentMassDeviation = profile.getExpectedFragmentMassDeviation();
        this.chemicalAlphabet = profile.getChemicalAlphabet().clone();
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

    public void setExpectedFragmentMassDeviation(Deviation expectedFragmentMassDeviation) {
        this.expectedFragmentMassDeviation = expectedFragmentMassDeviation;
    }

    public ChemicalAlphabet getChemicalAlphabet() {
        return chemicalAlphabet;
    }

    public void setChemicalAlphabet(ChemicalAlphabet chemicalAlphabet) {
        this.chemicalAlphabet = chemicalAlphabet;
    }
}
