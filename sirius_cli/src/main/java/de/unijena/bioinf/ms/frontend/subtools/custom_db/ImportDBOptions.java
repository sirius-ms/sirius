package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(name = "import", description = "Import compounds into an existing custom database.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
public class ImportDBOptions implements StandaloneTool<Workflow> {

    @CommandLine.Option(names = "--db", required = true,
            description = "Name of the custom database to import into.")
    String name = null;

    @CommandLine.Option(names = {"--buffer-size", "--buffer"}, defaultValue = "1000",
            description = {"Maximum number of downloaded/computed compounds to keep in memory before writing them to disk (into the db directory). Can be set higher when importing large files on a fast computer."})
    public int writeBuffer;

    @CommandLine.Parameters(arity = "1..*", description = {
            "Files or directories to import into the database.",
            "Supported formats: " + MsExperimentParser.DESCRIPTION,
            "Structures without spectra can be passed as a tab-separated file (.tsv or .csv, other separators are not supported) with fields [SMILES or InChI, id (optional), name (optional)].",
            "If the first line is a header naming a SMILES or InChI column, the columns are read by name instead, in any order and next to other columns. Common names are recognized, e.g. 'canonical_smiles', 'standard_inchi', 'compound_id', 'compound_name'.",
            "Directories will be recursively expanded."
    })
    public List<Path> input;

    @CommandLine.ArgGroup(exclusive = false, heading = "@|bold %n Biotransformations: %n|@")
    public BioTransformerOptions bioTransformerOptions;

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return new ImportDBWorkflow(this);
    }
}
