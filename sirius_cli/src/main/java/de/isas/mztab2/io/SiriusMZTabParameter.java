package de.isas.mztab2.io;

import de.isas.mztab2.model.CV;
import de.isas.mztab2.model.Database;
import de.isas.mztab2.model.OptColumnMapping;
import de.isas.mztab2.model.Parameter;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.chemdb.ChemicalDatabase;
import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.fingerid.db.SearchableDatabases;
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

    public static OptColumnMapping newOptColumn(SiriusMZTabParameter p, String value) {
        return new OptColumnMappingBuilder().forGlobal().withName(p.parameterName).build(value);
    }


    public final static Parameter SOFTWARE_SIRIUS = new Parameter()
            .name(PropertyManager.getProperty("de.unijena.bioinf.utils.errorReport.softwareName", "SIRIUS"))
            .value(PropertyManager.getProperty("de.unijena.bioinf.sirius.version"));

    public final static Parameter SOFTWARE_FINGER_ID = new Parameter()
            .name("CSI:FingerID")
            .value(PropertyManager.getProperty("de.unijena.bioinf.fingerid.version"));

    //todo should we integrade this service in DataSourceService.java
    public final static Database NO_DATABASE = new Database().id(1).param(new Parameter().name("no database").value("null"));
    public final static Database DE_NOVO = new Database().id(2).param(new Parameter().name("de novo"));
    public final static Database PUBCHEM = new Database().id(3)
            .prefix("CID")
            .uri("https://pubchem.ncbi.nlm.nih.gov/compound/")
            .version(PropertyManager.getProperty("de.unijena.bioinf.fingerid.db.date"))
            .param(new Parameter().name("SIRIUS PubChem Copy").value("CID"));

    public final static Parameter SCAN_POLARITY_ITEM_POSITIVE = new Parameter().cvLabel("MS").cvAccession("MS:1000130").name("positive scan");
    public final static Parameter SCAN_POLARITY_ITEM_NEGATIVE = new Parameter().cvLabel("MS").cvAccession("MS:1000129").name("negative scan");

    public final static CV DEFAULT_CV = new CV().id(1).label("MS").fullName("PSI-MS controlled vocabulary").version("4.1.16").uri("URL:https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo");

}