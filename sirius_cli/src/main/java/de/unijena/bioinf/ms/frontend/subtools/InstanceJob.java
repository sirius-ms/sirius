package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;

/**
 * This is a job for Scheduling an workflow synchronization
 * it should just be used to organize the workflow, wait for results and input dependencies
 * NOT for CPU intense task -> use a nested CPU job instead.
 * Note: Scheduler Jobs like this can be blocked without any problems so subjob submission is
 * NOT necessary and NOT recommended.
 */

public abstract class InstanceJob extends ToolChainJobImpl<Instance> implements ToolChainJob<Instance> {
    protected Instance input = null;

    public InstanceJob(JobSubmitter submitter) {
        super(submitter);
    }

    @Override
    public synchronized void handleFinishedRequiredJob(JJob required) {
        final Object r = required.result();
        if (r instanceof Instance)
            if (input == null || input.equals(r))
                input = (Instance) r;
    }


    @Override
    protected Instance compute() throws Exception {
        checkInput();
        computeAndAnnotateResult(input);
        updateProgress(0,100, 99, "DONE!");
        return input;
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        final Class<? extends DataAnnotation>[] ca = compoundComponentsToClear();
        if (input != null) {
            if (ca != null && ca.length > 0) input.clearCompoundCache(ca);
            final Class<? extends DataAnnotation>[] ra = formulaResultComponentsToClear();
            if (ra != null && ra.length > 0) input.clearFormulaResultsCache(ra);
        }
    }

    @Override
    public String identifier() {
        return super.identifier() + " | Instance: " + (input != null ? input.toString() : "Awaiting Instance!");
    }

    protected void checkInput() {
        if (input == null)
            throw new IllegalArgumentException("No Input available! Maybe a previous job could not provide the needed results due to failure.");
    }

    protected Class<? extends DataAnnotation>[] compoundComponentsToClear() {
        return null;
    }

    protected Class<? extends DataAnnotation>[] formulaResultComponentsToClear() {
        return null;
    }

    protected abstract void computeAndAnnotateResult(final @NotNull Instance expRes) throws Exception;

    @FunctionalInterface
    public interface Factory<T extends InstanceJob> extends ToolChainJob.Factory<T> {
        default T createToolJob(@NotNull JJob<Instance> inputProvidingJob) {
            return createToolJob(inputProvidingJob, SiriusJobs.getGlobalJobManager());
        }

        default T createToolJob(@NotNull JJob<Instance> inputProvidingJob, @NotNull JobSubmitter submitter) {
            final T job = makeJob(submitter);
            job.addRequiredJob(inputProvidingJob);
            return job;
        }
    }
}
