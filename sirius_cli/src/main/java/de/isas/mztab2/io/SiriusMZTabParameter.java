package de.isas.mztab2.io;

import de.isas.mztab2.model.OptColumnMapping;
import de.isas.mztab2.model.Parameter;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;

public enum SiriusMZTabParameter {

    //SIRIUS
    SIRIUS_SCORE("SIRIUS:isotope_score"),
    SIRIUS_ISOTOPE_SCORE("SIRIUS:isotope_score"),
    SIRIUS_TREE_SCORE("SIRIUS:tree_score"),
    SIRIUS_EXPL_INTENSITY("SIRIUS:explained_intensity"),
    SIRIUS_EXPL_PEAKS("SIRIUS:explained_peaks"),
    SIRIUS_MED_ABS_MASS_DEVIATION("SIRIUS:med_abs_mass_deviation"),
    SIRIUS_CANDIDATE_LIST("SIRIUS:candidates"),

    //FingerID
    FINGERID_SCORE("CSI:FingerID:score"),
    FINGERID_CONFIDENCE("CSI:FingerID:confidence"),
    FINGERID_TANIMOTO_SIMILARITY("CSI:FingerID:confidence"),
    FINGERID_CANDIDATE_LIST("CSI:FingerID:candidates"),
    FINGERID_FINGERPRINT_SOURCE("CSI:FingerID:fingerprints");

    //CANOPUS


    public final String cvLabel;
    public final String cvAccession;
    public final String parameterName;

    SiriusMZTabParameter(String parameterName) {
        this(null, null, parameterName);
    }

    SiriusMZTabParameter(String cvLabel, String cvAccession, String parameterName) {
        this.cvLabel = cvLabel;
        this.cvAccession = cvAccession;
        this.parameterName = parameterName;
    }

    public static Parameter newInstance(SiriusMZTabParameter p) {
        return new Parameter().cvLabel(p.cvLabel).cvAccession(p.cvAccession).name(p.parameterName);
    }

    public static Parameter newInstance(SiriusMZTabParameter p, String value) {
        return new Parameter().cvLabel(p.cvLabel).cvAccession(p.cvAccession).name(p.parameterName).value(value);
    }

    public static OptColumnMapping newOptColumnParameter(SiriusMZTabParameter p, String value) {
        return new OptColumnMapping().identifier(p.parameterName).param(new Parameter().cvLabel(p.cvLabel).cvAccession(p.cvAccession).name(p.parameterName).value(value));
    }


    public final static Parameter SOFTWARE_SIRIUS = new Parameter()
            .name(PropertyManager.getProperty("de.unijena.bioinf.utils.errorReport.softwareName", "SIRIUS"))
            .value(PropertyManager.getProperty("de.unijena.bioinf.sirius.version"));

    public final static Parameter SOFTWARE_FINGER_ID = new Parameter()
            .name("CSI:FingerID")
            .value(PropertyManager.getProperty("de.unijena.bioinf.fingerid.version"));


}
