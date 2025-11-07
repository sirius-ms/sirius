package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDatabases;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class AddDbWorkflowUtil {

    public static Workflow make(String location) {
        return () -> addDb(location);
    }

    public static void addDb(String location) {
        CdkFingerprintVersion version;
        try {
            version = ApplicationCore.WEB_API().getCDKChemDBFingerprintVersion();
            CustomDatabase newDb = CustomDatabases.open(location, true, version, true);
            CustomDBPropertyUtils.addDB(location, newDb.name());
        } catch (IOException e) {
            log.error("Error opening database.", e);
        }
    }
}
