package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;

/**
 * This is a job for Scheduling an workflow synchronization
 * it should just be used to organize the workflow, wait for results and input dependencies
 * NOT for CPU intense task -> use a nested CPU job instead.
 * Note: Scheduler Jobs like this can be blocked without any problems so subjob submission is
 * NOT necessary and NOT recommended.
 */

public abstract class InstanceJob extends BasicDependentJJob<ExperimentResult> implements SubToolJob {
    private ExperimentResult input = null;

    public InstanceJob() {
        super(JobType.SCHEDULER);
    }

    @Override
    public synchronized void handleFinishedRequiredJob(JJob required) {
        if (input == null) {
            final Object r = required.result();
            if (r instanceof ExperimentResult)
                input = (ExperimentResult) r;
        }
    }


    @Override
    protected ExperimentResult compute() throws Exception {
        checkInput();
        computeAndAnnotateResult(input);
        return input;
    }

    protected void checkInput() {
        if (input == null)
            throw new IllegalArgumentException("No Input given!");
    }

    protected abstract void computeAndAnnotateResult(final @NotNull ExperimentResult expRes) throws Exception;


    /*@Override
    protected void cleanup() {
        super.cleanup();
        input = null;
    }*/

    @FunctionalInterface
    public interface Factory<T extends InstanceJob> {
        default T createToolJob(JJob<ExperimentResult> inputProvidingJob) {
            final T job = makeJob();
            job.addRequiredJob(inputProvidingJob);
            return job;
        }

        T makeJob();
    }
}
