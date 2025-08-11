package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import picocli.CommandLine;

/**
 * Command that cannot be executed on its own, only with a subcommand.
 */
public abstract class ParentCommand implements StandaloneTool<Workflow> {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec ownSpec;

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return () -> {
            String subtools = String.join(",", ownSpec.subcommands().keySet());
            System.out.printf("Please specify one of the subcommands for %s: [%s]%n", ownSpec.name(), subtools);
        };
    }
}
