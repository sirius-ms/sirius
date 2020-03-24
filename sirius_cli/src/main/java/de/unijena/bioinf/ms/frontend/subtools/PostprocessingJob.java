package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.jjobs.BasicJJob;

/**
 * Only one postprocessing Subtool is allowed per Workflow. They can only run in last position
 * These tools are intended to do some cleanup tasks, or writing summaries and closing the project.
 */
public abstract class PostprocessingJob<P> extends BasicJJob<P> implements SubToolJob {
    public PostprocessingJob() {
                super(JobType.SCHEDULER);
    }
}
