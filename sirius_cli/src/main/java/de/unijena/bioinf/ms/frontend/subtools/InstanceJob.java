package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.io.projectspace.Instance;
import org.jetbrains.annotations.NotNull;

/**
 * This is a job for Scheduling an workflow synchronization
 * it should just be used to organize the workflow, wait for results and input dependencies
 * NOT for CPU intense task -> use a nested CPU job instead.
 * Note: Scheduler Jobs like this can be blocked without any problems so subjob submission is
 * NOT necessary and NOT recommended.
 */

public abstract class InstanceJob extends BasicDependentJJob<Instance> implements SubToolJob {
    private Instance input = null;

    public InstanceJob() {
        super(JobType.SCHEDULER);
    }

    @Override
    public synchronized void handleFinishedRequiredJob(JJob required) {
        if (input == null) {
            final Object r = required.result();
            if (r instanceof Instance)
                input = (Instance) r;
        }
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
        return super.identifier() + " | Instance: " + (input != null ? input.toString() : "NULL");
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
    public interface Factory<T extends InstanceJob> {
        default T createToolJob(JJob<Instance> inputProvidingJob) {
            final T job = makeJob();
            job.addRequiredJob(inputProvidingJob);
            return job;
        }

        T makeJob();
    }
}
