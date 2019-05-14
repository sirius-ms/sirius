package de.unijena.bioinf.ms.frontend.parameters;

import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.ExperimentResult;

import java.util.concurrent.ExecutionException;

/**
 * This is a job for Scheduling an workflow synchronization
 * it should just be used to organize the workflow, wait for results and input dependencies
 * NOT for CPU intense task -> use a nested CPU job instead.
 * Note: Scheduler Jobs like this can be blocked without any problems so subjob submission is
 * NOT necessary and NOT recommended.
 */

public abstract class InstanceJob extends BasicDependentJJob<ExperimentResult> {
    private JJob<ExperimentResult> inputProvidingJob = null;
    private ExperimentResult input = null;

    public InstanceJob() {
        super(JobType.SCHEDULER);
    }


    public void setInputProvidingJob(JJob<ExperimentResult> inputProvidingJob) {
        addRequiredJob(inputProvidingJob);
        this.inputProvidingJob = inputProvidingJob;
    }

    protected ExperimentResult awaitInput() throws ExecutionException {
        if (input != null) {
            return input;
        }
        if (inputProvidingJob != null) {
            input = inputProvidingJob.awaitResult();
            return input;
        }

        throw new ExecutionException(new IllegalArgumentException("Neither an input nor an input providing job was provided"));
    }


    protected ExperimentResult getInput() {
        return input;
    }

    protected JJob<ExperimentResult> getInputProvidingJob() {
        return inputProvidingJob;
    }


    @FunctionalInterface
    public interface Factory<T extends InstanceJob> {
        default T createToolJob(JJob<ExperimentResult> inputProvidingJob) {
            final T job = makeJob();
            job.setInputProvidingJob(inputProvidingJob);
            return job;
        }

        T makeJob();
    }
}
