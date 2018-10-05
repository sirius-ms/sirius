package de.isas.mztab2.io;

import de.isas.mztab2.model.OptColumnMapping;
import de.isas.mztab2.model.Parameter;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import uk.ac.ebi.pride.jmztab2.model.OptColumnMappingBuilder;

public enum SiriusMZTabParameter {
    //SIRIUS
    SIRIUS_SCORE("SIRIUS:score"),
    SIRIUS_ISOTOPE_SCORE("SIRIUS:isotope_score"),
    SIRIUS_TREE_SCORE("SIRIUS:tree_score"),
    SIRIUS_EXPL_INTENSITY_RATIO("SIRIUS:explained_intensity_ratio"),
    SIRIUS_EXPL_PEAKS("SIRIUS:num_explained_peaks"),
    SIRIUS_EXPL_PEAKS_RATIO("SIRIUS:explained_peaks_ratio"),
    SIRIUS_MED_ABS_MASS_DEVIATION("SIRIUS:med_abs_mass_deviation"),

    SIRIUS_ANNOTATED_SPECTRA_LOCATION("SIRIUS:annotated_spectra_location"),
    SIRIUS_TREE_LOCATION("SIRIUS:tree_location"),
    SIRIUS_CANDIDATE_LOCATION("SIRIUS:candidate_location"),

    //FingerID
    FINGERID_SCORE("CSI:FingerID:score"),
    FINGERID_CONFIDENCE("CSI:FingerID:confidence"),
    FINGERID_TANIMOTO_SIMILARITY("CSI:FingerID:confidence"),

    FINGERID_CANDIDATE_LOCATION("CSI:FingerID:candidate_location"),
    FINGERID_FINGERPRINT_LOCATION("CSI:FingerID:fingerprint_location");

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

    /*public static OptColumnMapping newOptColumnParameter(SiriusMZTabParameter p, String value) {

    }*/

    public static OptColumnMapping newOptColumn(SiriusMZTabParameter p, String value) {
        return new OptColumnMappingBuilder().forGlobal().withName(p.parameterName).build(value);
    }


    public final static Parameter SOFTWARE_SIRIUS = new Parameter()
            .name(PropertyManager.getProperty("de.unijena.bioinf.utils.errorReport.softwareName", "SIRIUS"))
            .value(PropertyManager.getProperty("de.unijena.bioinf.sirius.version"));

    public final static Parameter SOFTWARE_FINGER_ID = new Parameter()
            .name("CSI:FingerID")
            .value(PropertyManager.getProperty("de.unijena.bioinf.fingerid.version"));


}
