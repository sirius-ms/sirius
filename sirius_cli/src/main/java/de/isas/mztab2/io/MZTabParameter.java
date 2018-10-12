package de.isas.mztab2.io;

import de.isas.mztab2.model.Parameter;
import de.isas.mztab2.model.SmallMoleculeEvidence;

public enum MZTabParameter {
    MS_LEVEL("MS", "MS:1000511", SmallMoleculeEvidence.Properties.msLevel.getPropertyName()),
    RELIABILITY("MS", "MS:1002955", "hr-ms compound identification confidence level"/*SmallMoleculeSummary.Properties.reliability.getPropertyName()*/);

    public final String cvLabel;
    public final String cvAccession;
    public final String parameterName;

    MZTabParameter(String cvLabel, String cvAccession, String name) {
        this.cvLabel = cvLabel;
        this.cvAccession = cvAccession;
        this.parameterName = name;
    }

    public static Parameter newInstance(MZTabParameter p) {
        return new Parameter().cvLabel(p.cvLabel).cvAccession(p.cvAccession).name(p.parameterName);
    }
}