package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "add", description = "Add an existing custom database file.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
public class AddDBOptions implements StandaloneTool<Workflow> {

    @CommandLine.Option(names = "--location", required = true,
            description = "Location of the custom database to add to SIRIUS.")
    String location = null;

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return AddDbWorkflowUtil.make(location);
    }
}
