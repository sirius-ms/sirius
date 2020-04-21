package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobSubmitter;
import org.jetbrains.annotations.NotNull;

public abstract class ToolChainJobImpl<R> extends BasicDependentJJob<R> implements ToolChainJob<R>, JobSubmitter {
    private final JobSubmitter submitter;

    public ToolChainJobImpl(@NotNull JobSubmitter submitter) {
        super(JobType.SCHEDULER);
        this.submitter = submitter;
    }

    public ToolChainJobImpl(@NotNull JobSubmitter submitter, @NotNull ReqJobFailBehaviour failBehaviour) {
        super(JobType.SCHEDULER, failBehaviour);
        this.submitter = submitter;
    }

    @Override
    public <Job extends JJob<Result>, Result> Job submitJob(Job job) {
        return submitter.submitJob(job);
    }
}
