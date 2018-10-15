package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class FingerIdOptions {
    String CONSIDER_ALL_FORMULAS = "all";

    @Option(names = "-d", description = "search formulas in given database: all, pubchem, bio, kegg, hmdb")
    String database = CONSIDER_ALL_FORMULAS;

    @Option(names = {"--fingerid-db", "--fingerid_db", "--fingeriddb"}, description = "search structure in given database. By default the same database for molecular formula search is also used for structure search. If no database is used for molecular formula search, PubChem is used for structure search. Accepts also a filepath to a valid database directory.")
    String fingerIdDb = null;

    @Option(names = "-F, --fingerid", description = "search structure with CSI:FingerID")
    boolean fingerid = false;

    @Option(names = {"--webservice-info", "-W"}, description = "information about connection of CSI:FingerID Webservice")
    boolean fingeridInfo = false;

    @Option(names = {"--fingerid-predictors", "-P"}, description = "Predictors used to search structure with CSI:FingerID")
    List<UserDefineablePredictorType> getPredictors = Collections.singletonList(UserDefineablePredictorType.CSI_FINGERID);

    @Option(names = "--generate-custom-db", description = "EXPERIMENTAL FEATURE: generate a custom compound database. Ignore all other options. Import compounds from all given files. Usage: sirius --generate-custom-db [DATABASENAME] [INPUTFILE1] [INPUTFILE2] ... ")
    String generatingCompoundDatabase = null;

    @Option(names = {"--experimental-canopus"}, hidden = true) //todo mak like zodiac and fingerid
    File experimentalCanopus = null;


    /*
    @Option(description = "output predicted fingerprint")
    public File getPredict();
    */
}
