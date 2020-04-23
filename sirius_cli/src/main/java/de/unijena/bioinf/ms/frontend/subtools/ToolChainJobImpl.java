package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.annotations.RecomputeResults;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class ToolChainJobImpl<R> extends BasicDependentJJob<R> implements ToolChainJob<R>, JobSubmitter {
    private final JobSubmitter submitter;
    private Consumer<Instance> invalidator;

    public ToolChainJobImpl(@NotNull JobSubmitter submitter) {
        super(JobType.SCHEDULER);
        this.submitter = submitter;
    }

    public ToolChainJobImpl(@NotNull JobSubmitter submitter, @NotNull ReqJobFailBehaviour failBehaviour) {
        super(JobType.SCHEDULER, failBehaviour);
        this.submitter = submitter;
    }

    @Override
    public void setInvalidator(Consumer<Instance> invalidator) {
        this.invalidator = invalidator;
    }

    @Override
    public void invalidateResults(@NotNull Instance inst) {
        if (invalidator != null)
            invalidator.accept(inst);
    }

    @Override
    public <Job extends JJob<Result>, Result> Job submitJob(Job job) {
        return submitter.submitJob(job);
    }
}
