package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDatabases;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.IOException;

@Slf4j
@CommandLine.Command(name = "add", description = "Add an existing custom database file.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
public class AddDBOptions implements StandaloneTool<Workflow> {

    @CommandLine.Option(names = "--location", required = true,
            description = "Location of the custom database to add to SIRIUS.")
    String location = null;

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return () -> {
            CdkFingerprintVersion version;
            try {
                version = ApplicationCore.WEB_API.getCDKChemDBFingerprintVersion();
                CustomDatabase newDb = CustomDatabases.open(location, true, version, true);
                CustomDBPropertyUtils.addDB(location, newDb.name());
            } catch (IOException e) {
                log.error("Error opening database.", e);
            }
        };
    }
}
