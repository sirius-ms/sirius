package de.unijena.bioinf.ms.cli;

import com.lexicalscope.jewel.cli.Option;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;

import java.io.File;
import java.util.List;

public interface FingerIdOptions extends SiriusOptions {
    String CONSIDER_ALL_FORMULAS = "all";

    @Option(shortName = "d", defaultValue = CONSIDER_ALL_FORMULAS, description = "search formulas in given database: all, pubchem, bio, kegg, hmdb")
    String getDatabase();

    @Option(longName = {"fingerid-db", "fingerid_db"}, description = "search structure in given database. By default the same database for molecular formula search is also used for structure search. If no database is used for molecular formula search, PubChem is used for structure search. Accepts also a filepath to a valid database directory.", defaultToNull = true)
    String getFingerIdDb();

    @Option(shortName = "F", description = "search structure with CSI:FingerID")
    boolean isFingerid();

    @Option(longName = "webservice-info", shortName = "W", description = "information about connection of CSI:FingerID Webservice")
    boolean isFingeridInfo();

    @Option(longName = {"fingerid-predictors"}, shortName = "P", description = "Predictors used to search structure with CSI:FingerID", defaultValue = "CSI_FINGERID")
    List<UserDefineablePredictorType> getPredictors();

    @Option(longName = "generate-custom-db", description = "EXPERIMENTAL FEATURE: generate a custom compound database. Ignore all other options. Import compounds from all given files. Usage: sirius --generate-custom-db [DATABASENAME] [INPUTFILE1] [INPUTFILE2] ... ", defaultToNull = true)
    String getGeneratingCompoundDatabase();

    @Option(longName = "experimental-canopus", hidden = true, defaultToNull = true)
    File getExperimentalCanopus();


    /*
    @Option(description = "output predicted fingerprint")
    public File getPredict();
    */
}
