package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;

public class MutableMeasurementProfile implements MeasurementProfile{

    private Deviation allowedMassDeviation, standardMs1MassDeviation, standardMs2MassDeviation, standardMassDifferenceDeviation;
    private FormulaConstraints formulaConstraints;
    private double expectedIntensityDeviation;
    private double medianNoiseIntensity;

    public static MeasurementProfile merge(MeasurementProfile a1, MeasurementProfile a2) {
        final MutableMeasurementProfile profile = new MutableMeasurementProfile(a2);
        if (profile.getAllowedMassDeviation() == null) profile.setAllowedMassDeviation(a1.getAllowedMassDeviation());
        if (profile.getExpectedIntensityDeviation()==0) profile.setExpectedIntensityDeviation(a1.getExpectedIntensityDeviation());
        if (profile.getStandardMassDifferenceDeviation()==null) profile.setStandardMassDifferenceDeviation(a1.getStandardMassDifferenceDeviation());
        if (profile.getStandardMs1MassDeviation()==null) profile.setStandardMs1MassDeviation(a1.getStandardMs1MassDeviation());
        if (profile.getStandardMs2MassDeviation()==null) profile.setStandardMs2MassDeviation(a1.getStandardMs2MassDeviation());
        if (profile.getFormulaConstraints()==null) profile.setFormulaConstraints(a1.getFormulaConstraints());
        return profile;
    }

    public MutableMeasurementProfile() {
    }

    public MutableMeasurementProfile(MeasurementProfile profile) {
        this(profile.getAllowedMassDeviation(), profile.getStandardMs1MassDeviation(), profile.getStandardMs2MassDeviation(), profile.getStandardMassDifferenceDeviation(),
                profile.getFormulaConstraints(), profile.getExpectedIntensityDeviation(), profile.getMedianNoiseIntensity());
    }

    public MutableMeasurementProfile(Deviation allowedMassDeviation, Deviation standardMs1MassDeviation, Deviation standardMs2MassDeviation,
                                     Deviation standardMassDifferenceDeviation, FormulaConstraints formulaConstraints, double expectedIntensityDeviation, double medianNoiseIntensity) {
        this.allowedMassDeviation = allowedMassDeviation;
        this.standardMs1MassDeviation = standardMs1MassDeviation;
        this.standardMs2MassDeviation = standardMs2MassDeviation;
        this.standardMassDifferenceDeviation = standardMassDifferenceDeviation;
        this.formulaConstraints = formulaConstraints;
        this.expectedIntensityDeviation = expectedIntensityDeviation;
        this.medianNoiseIntensity = medianNoiseIntensity;
    }

    public Deviation getAllowedMassDeviation() {
        return allowedMassDeviation;
    }

    public void setAllowedMassDeviation(Deviation allowedMassDeviation) {
        this.allowedMassDeviation = allowedMassDeviation;
    }

    public Deviation getStandardMs1MassDeviation() {
        return standardMs1MassDeviation;
    }

    public void setStandardMs1MassDeviation(Deviation standardMs1MassDeviation) {
        this.standardMs1MassDeviation = standardMs1MassDeviation;
    }

    public Deviation getStandardMs2MassDeviation() {
        return standardMs2MassDeviation;
    }

    public void setStandardMs2MassDeviation(Deviation standardMs2MassDeviation) {
        this.standardMs2MassDeviation = standardMs2MassDeviation;
    }

    public Deviation getStandardMassDifferenceDeviation() {
        return standardMassDifferenceDeviation;
    }

    public void setStandardMassDifferenceDeviation(Deviation standardMassDifferenceDeviation) {
        this.standardMassDifferenceDeviation = standardMassDifferenceDeviation;
    }

    public FormulaConstraints getFormulaConstraints() {
        return formulaConstraints;
    }

    public void setFormulaConstraints(FormulaConstraints formulaConstraints) {
        this.formulaConstraints = formulaConstraints;
    }

    public double getExpectedIntensityDeviation() {
        return expectedIntensityDeviation;
    }

    public void setExpectedIntensityDeviation(double expectedIntensityDeviation) {
        this.expectedIntensityDeviation = expectedIntensityDeviation;
    }

    public double getMedianNoiseIntensity() {
        return medianNoiseIntensity;
    }

    public void setMedianNoiseIntensity(double medianNoiseIntensity) {
        this.medianNoiseIntensity = medianNoiseIntensity;
    }
}
