package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;

public interface SingletonTool {

    Workflow makeSingletonWorkflow(PreprocessingJob preproJob, ProjectSpaceManager projectSpace, ParameterConfig config);
}
