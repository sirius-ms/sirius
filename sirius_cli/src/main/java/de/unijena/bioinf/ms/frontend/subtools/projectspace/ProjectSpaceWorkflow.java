package de.unijena.bioinf.ms.frontend.subtools.projectspace;

import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;

public class ProjectSpaceWorkflow implements Workflow {


    private final RootOptions<?, ?> rootOptions;

    public ProjectSpaceWorkflow(RootOptions<?, ?> rootOptions) {
        this.rootOptions = rootOptions;
    }

    @Override
    public void run() {

    }
}
