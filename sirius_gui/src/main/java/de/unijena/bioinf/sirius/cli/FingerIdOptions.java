package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.Option;

public interface FingerIdOptions extends SiriusGUIOptions {

    @Option(shortName = "d", defaultValue = "all", description = "search formulas in given database: all, pubchem, bio, kegg, hmdb")
    String getDatabase();

    @Option(longName = {"fingerid-db","fingerid_db"}, description = "search structure in given database. By default the same database for molecular formula search is also used for structure search. If no database is used for molecular formula search, PubChem is used for structure search. Accepts also a filepath to a valid database directory.", defaultToNull = true)
    String getFingerIdDb();

    @Option(shortName = "F", description = "search structure with CSI:FingerId")
    boolean isFingerid();

    @Option(longName = "generate-custom-db", description = "EXPERIMENTAL FEATURE: generate a custom compound database. Ignore all other options. Import compounds from all given files. Usage: sirius --generate-custom-db [DATABASENAME] [INPUTFILE1] [INPUTFILE2] ... ", defaultToNull = true)
    String getGeneratingCompoundDatabase();

    /*
    @Option(description = "output predicted fingerprint")
    public File getPredict();
    */
}
