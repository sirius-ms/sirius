package de.unijena.bioinf.ms.frontend.subtools.custom_db.export;

import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.nio.file.Path;

@Slf4j
@CommandLine.Command(name = "export", description = "Export custom database.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
public class ExportDBOptions implements StandaloneTool<Workflow> {
    @CommandLine.Option(names = "--db", required = true,
            description = "Name of the custom database to export.")
    String name = null;

    @CommandLine.Option(names = {"--output", "-o"}, description = {"Specify location for exported file.", "By default files are written to the working directory of SIRIUS."})
    protected Path location;

    @CommandLine.Option(names = {"--format"}, description = {"Output format for exported file: ${COMPLETION-CANDIDATES}."}, defaultValue = "tsv")
    protected Format format;

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return new ExportCustomDBJJob(name, location, format);
    }
}
