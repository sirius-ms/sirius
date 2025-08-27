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
import java.util.Optional;

@Slf4j
@CommandLine.Command(name = "remove", description = "Remove the given custom database.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
public class RemoveDBOptions implements StandaloneTool<Workflow> {
    @CommandLine.Option(names = "--db", required = true,
            description = "Name of the custom database to remove from SIRIUS.")
    String name = null;

    @CommandLine.Option(names = {"--delete", "-d"}, defaultValue = "false",
            description = "Delete removed custom database from filesystem/server.")
    boolean delete;

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return () -> {
            CdkFingerprintVersion version;
            try {
                version = ApplicationCore.WEB_API().getCDKChemDBFingerprintVersion();
            } catch (IOException e) {
                log.error("Error getting fingerprint version", e);
                return;
            }
            Optional<String> maybeLocation = CustomDBPropertyUtils.getLocationByName(name);
            if (maybeLocation.isEmpty()) {
                log.error("Database {} not found.", name);
            } else {
                String location = maybeLocation.get();
                try {
                    CustomDatabase db = CustomDatabases.open(location, version, true);
                    CustomDatabases.remove(db, delete);
                } catch (Exception e) {
                    log.error("Error removing database {}", name, e);
                }
                CustomDBPropertyUtils.removeDBbyLocation(location);
            }
        };
    }
}
