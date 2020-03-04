package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;

public interface StandaloneTool<W extends Workflow> {

    W makeWorkflow(RootOptions<?,?, ?> rootOptions, ParameterConfig config);
}
