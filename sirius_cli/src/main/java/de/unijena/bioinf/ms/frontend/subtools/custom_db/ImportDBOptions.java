package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.chemdb.custom.CustomDatabases;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(name = "import", description = "Import compounds into a new or existing custom database.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
public class ImportDBOptions implements StandaloneTool<Workflow> {

    @CommandLine.Option(names = "--location", required = true,
            description = {"Location of the custom database to import into.",
                    "An absolute local path to a new database file file to be created (file name must end with .siriusdb)",
                    "If no input data is given (--input), the database will just be added to SIRIUS",
                    "The added db will also be available in the GUI."}, order = 201)
    public String location = null;

    @CommandLine.Option(names = "--name", order = 202,
            description = {"Name/Identifier of the custom database.",
                    "If not given, filename from location will be used."})
    public void setName(String name) {
        this.name = CustomDatabases.sanitizeDbName(name);
    }
    public String name = null;

    @CommandLine.Option(names = "--displayName", order = 203,
            description = {"Displayable name of the custom database.",
                    "This is the preferred name to be shown in the GUI. Maximum Length: 15 characters.",
                    "If not given name will be used."})
    public void setDisplayName(String displayName) {
        if (displayName.length() > 15)
            throw new CommandLine.PicocliException("Maximum allowed length for display names is 15 characters.");
        this.displayName = displayName;
    }
    public String displayName = null;

    @CommandLine.Option(names = {"--buffer-size", "--buffer"}, defaultValue = "1000",
            description = {"Maximum number of downloaded/computed compounds to keep in memory before writing them to disk (into the db directory). Can be set higher when importing large files on a fast computer."},
            order = 210)
    public int writeBuffer;

    @CommandLine.Option(names = {"--input", "-i"}, split = ",", description = {
            "Files or directories to import into the database.",
            "Supported formats: " + MsExperimentParser.DESCRIPTION,
            "Structures without spectra can be passed as a tab-separated (.tsv) file with fields [SMILES, id (optional), name (optional)].",
            "Directories will be recursively expanded."
    }, order = 220)
    public List<Path> input;

    @CommandLine.ArgGroup(exclusive = false, heading = "@|bold %n Biotransformations: %n|@", order = 150)
    public BioTransformerOptions bioTransformerOptions;

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return new ImportDBWorkflow(this);
    }
}
