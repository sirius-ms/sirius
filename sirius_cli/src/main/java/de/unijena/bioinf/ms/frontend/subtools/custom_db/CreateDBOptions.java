package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDatabaseSettings;
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
import java.util.List;

@Slf4j
@CommandLine.Command(name = "create", description = "Create a new empty custom database.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
public class CreateDBOptions implements StandaloneTool<Workflow> {

    @CommandLine.Option(names = "--location", required = true,
            description = {"Location of the custom database to add to SIRIUS.",
                    "An absolute local path to a new database file file to be created (file name must end with .siriusdb)"})
    private String location = null;

    @CommandLine.Option(names = "--name",
            description = {"Name/Identifier of the custom database.",
                    "If not given, filename from location will be used."})
    public void setName(String name) {
        this.name = CustomDatabases.sanitizeDbName(name);
    }
    private String name = null;

    @CommandLine.Option(names = "--displayName",
            description = {"Displayable name of the custom database.",
                    "This is the preferred name to be shown in the GUI. Maximum Length: 15 characters.",
                    "If not given, name will be used."})
    public void setDisplayName(String displayName) {
        if (displayName.length() > 15)
            throw new CommandLine.PicocliException("Maximum allowed length for display names is 15 characters.");
        this.displayName = displayName;
    }
    private String displayName = null;


    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return () -> {
            CdkFingerprintVersion version;
            try {
                version = ApplicationCore.WEB_API.getCDKChemDBFingerprintVersion();

                CustomDatabaseSettings settings = CustomDatabaseSettings.builder()
                        .usedFingerprints(List.of(version.getUsedFingerprints()))
                        .schemaVersion(CustomDatabase.CUSTOM_DATABASE_SCHEMA)
                        .name(name != null && !name.isBlank() ? name : CustomDatabases.sanitizeDbName(CustomDBPropertyUtils.getDBName(location)))
                        .displayName(displayName)
                        .matchRtOfReferenceSpectra(false)
                        .statistics(new CustomDatabaseSettings.Statistics())
                        .build();

                CustomDatabase newDb = CustomDatabases.create(location, settings, version, true);
                CustomDBPropertyUtils.addDB(location, newDb.name());
            } catch (IOException e) {
                log.error("Error creating database.", e);
            }
        };
    }
}
