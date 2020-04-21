package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.jjobs.BasicJJob;

/**
 * Only one preprocessing Subtool is allowed per Workflow. They can only run in first position
 * These tools are intended to transform some non standard input into a list of Instances
 * The default here is to write the results to the project space that will be used for th rest of the workflow
 */
public abstract class PreprocessingJob<P extends Iterable<? extends Instance>> extends BasicJJob<P> {
//    protected InputFilesOptions input;
//    protected ProjectSpaceManager space;

    public PreprocessingJob() {
                super(JobType.SCHEDULER);

    }

    /*public PreprocessingJob(@Nullable InputFilesOptions input, @Nullable ProjectSpaceManager space) {
        super(JobType.SCHEDULER);
//        this.input = input;
//        this.space = space;
    }*/

/*    public void setInput(InputFilesOptions input) {
        this.input = input;
    }

    public void setProjectSpace(ProjectSpaceManager space) {
        this.space = space;
    }*/
}
