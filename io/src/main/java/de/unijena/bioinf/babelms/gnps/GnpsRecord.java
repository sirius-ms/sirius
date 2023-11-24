package de.unijena.bioinf.babelms.gnps;

import lombok.Data;

@Data
public class GnpsRecord {
    private String spectrum_id;
    private String source_file;
    private String task;
    private String scan;
    private String ms_level;
    private String library_membership;
    private String spectrum_status;
    private String peaks_json;
    private String splash;
    private String submit_user;
    private String Compound_Name;
    private String Ion_Source;
    private String Compound_Source;
    private String Instrument;
    private String PI;
    private String Data_Collector;
    private String Adduct;
    private String Scan;
    private String Precursor_MZ;
    private String ExactMass;
    private String Charge;
    private String CAS_Number;
    private String Pubmed_ID;
    private String Smiles;
    private String INCHI;
    private String INCHI_AUX;
    private String Library_Class;
    private String SpectrumID;
    private String Ion_Mode;
    private String create_time;
    private String task_id;
    private String user_id;
    private String InChIKey_smiles;
    private String InChIKey_inchi;
    private String Formula_smiles;
    private String Formula_inchi;
    private String url;
}
