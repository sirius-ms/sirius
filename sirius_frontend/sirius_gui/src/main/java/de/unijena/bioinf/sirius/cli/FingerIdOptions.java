package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.Option;

public interface FingerIdOptions extends SiriusGUIOptions {

    @Option(shortName = "d", defaultValue = "all", description = "search formulas in given database: all, pubchem, bio, kegg, hmdb")
    public String getDatabase();

    @Option(shortName = "-F", description = "search structure with CSI:FingerId")
    public boolean isFingerid();

    @Option(longName = "generate-custom-db", description = "EXPERIMENTAL FEATURE: generate a custom compound database. Ignore all other options. Import compounds from all given files. Takes the name of the database as parameter.", defaultToNull = true, hidden = true)
    public String isGeneratingCompoundDatabase();

    /*
    @Option(description = "output predicted fingerprint")
    public File getPredict();
    */
}
