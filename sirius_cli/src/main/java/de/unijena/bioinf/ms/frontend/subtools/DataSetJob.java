package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class DataSetJob extends BasicDependentJJob<Iterable<ExperimentResult>> implements SubToolJob {
    private List<JJob> failedInstances = new ArrayList<>();
    private List<ExperimentResult> successfulInstances = new ArrayList<>();

    public DataSetJob() {
        super(JobType.SCHEDULER, ReqJobFailBehaviour.WARN);
    }

    @Override
    protected Iterable<ExperimentResult> compute() throws Exception {
        checkInputs();
        computeAndAnnotateResult(successfulInstances);
        return successfulInstances;
    }

    protected void checkInputs() {
        if (successfulInstances == null || successfulInstances.isEmpty())
            throw new IllegalArgumentException("No Input found, all dependend Jobs are failed");
        if (!failedInstances.isEmpty())
            LOG().warn("There are " + failedInstances.size() + "failed InputProvidingJobs!");
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        failedInstances = null;
    }

    @Override
    public synchronized void handleFinishedRequiredJob(JJob required) {
        final Object r = required.result();
        if (r instanceof ExperimentResult)
            successfulInstances.add((ExperimentResult) r);

    }


    protected abstract void computeAndAnnotateResult(final @NotNull List<ExperimentResult> expRes) throws Exception;

    public List<JJob> getFailedInstances() {
        return failedInstances;
    }

    public boolean hasFailedInstances() {
        return failedInstances != null && !failedInstances.isEmpty();
    }

    @FunctionalInterface
    public interface Factory<T extends DataSetJob> {
        default T createToolJob(Iterable<JJob<ExperimentResult>> dataSet) {
            final T job = makeJob();
            dataSet.forEach(job::addRequiredJob);
            return job;
        }

        T makeJob();
    }


}
