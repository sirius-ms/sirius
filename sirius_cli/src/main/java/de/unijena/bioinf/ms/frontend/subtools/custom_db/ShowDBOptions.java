package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "show", description = "Print information about all or the given custom database.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false)
public class ShowDBOptions implements StandaloneTool<Workflow> {

    @CommandLine.Option(names = {"--db"}, description = "Show information only about the given custom database (name or location).", order = 110)
    String db = null;

    @CommandLine.Option(names = {"--details"}, description = "Show detailed (technical) information.", order = 120)
    public boolean details;

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return ShowWorkflowUtil.make(db, details);
    }
}
