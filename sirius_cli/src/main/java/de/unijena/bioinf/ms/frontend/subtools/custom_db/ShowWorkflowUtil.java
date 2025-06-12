package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDatabaseSettings;
import de.unijena.bioinf.chemdb.custom.CustomDatabases;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class ShowWorkflowUtil {

    public static Workflow make(String db, boolean detailed) {
        return () -> {
            final CdkFingerprintVersion version;
            try {
                version = ApplicationCore.WEB_API.getCDKChemDBFingerprintVersion();
            } catch (IOException e) {
                log.error("Error getting fingerprint version", e);
                return;
            }
            if (db == null || db.isBlank()) {
                showAllDBs(version, detailed);
            } else {
                showDB(db, version, detailed);
            }
        };
    }

    private static void showAllDBs(CdkFingerprintVersion version, boolean detailed) {
        LinkedHashMap<String, String> customDBs = CustomDBPropertyUtils.getCustomDBs();

        if (customDBs.isEmpty()) {
            System.out.println("==> No Custom database found!");
            return;
        }

        for (Map.Entry<String, String> e : customDBs.entrySet()) {
            String location = e.getKey();
            String name = e.getValue();
            try {
                CustomDatabase db = CustomDatabases.open(location, version, true);
                printDBInfo(db, detailed);
            } catch (Exception ex) {
                printDBError(location, name, ex.getMessage());
            }
        }
    }

    private static void showDB(String dbName, CdkFingerprintVersion version, boolean detailed) {
        Optional<String> maybeLocation = CustomDBPropertyUtils.getLocationByName(dbName);
        if (maybeLocation.isEmpty()) {
            log.error("Database {} not found.", dbName);
        } else {
            String location = maybeLocation.get();
            try {
                CustomDatabase db = CustomDatabases.open(location, version, true);
                printDBInfo(db, detailed);
            } catch (Exception ex) {
                printDBError(location, CustomDBPropertyUtils.getCustomDBs().get(location), ex.getMessage());
            }
        }
    }

    private static void printDBInfo(CustomDatabase db, boolean detailed) {
        CustomDatabaseSettings s = db.getSettings();
        System.out.println("##########  BEGIN DB INFO  ##########");
        System.out.println("Name: " + db.name());
        System.out.println("Display Name: " + db.displayName());
        System.out.println("Location: " + db.storageLocation());
        System.out.println("Number of Formulas: " + s.getStatistics().getFormulas());
        System.out.println("Number of Structures: " + s.getStatistics().getCompounds());
        System.out.println("Number of Reference spectra: " + s.getStatistics().getSpectra());
        if (detailed) {
            System.out.println("Version: " + db.getDatabaseVersion());
            System.out.println("Schema Version: " + s.getSchemaVersion());
            System.out.println("FilterFlag: " + db.getFilterFlag());
            System.out.println("Used Fingerprints: [ '" + s.getUsedFingerprints().stream().map(Enum::name).collect(Collectors.joining("','")) + "' ]");
        }
        System.out.println("###############  END  ###############");
        System.out.println();
        System.out.println();
    }

    private static void printDBError(String location, String name, String error) {
        System.out.println("#####  Error Opening Database  #####");
        System.out.println("Name: " + name);
        System.out.println("Location: " + location);
        System.out.println("Error: " + error);
        System.out.println("###############  END  ###############");
        System.out.println();
        System.out.println();
    }
}
