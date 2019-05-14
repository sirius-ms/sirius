package de.unijena.bioinf.ms.frontend.parameters;

import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.ExperimentResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class DataSetJob extends BasicDependentJJob<Iterable<ExperimentResult>> {
    private LinkedHashSet<JJob<ExperimentResult>> inputProvidingJobs = new LinkedHashSet<>();
    private List<JJob<ExperimentResult>> failedInstances = null;
    private List<ExperimentResult> successfulInstances = null;

    public DataSetJob() {
        super(JobType.SCHEDULER);
    }

    public void addInputProvidingJobs(Iterable<JJob<ExperimentResult>> providingJobs) {
        providingJobs.forEach(this::addInputProvidingJob);
    }

    public void addInputProvidingJob(JJob<ExperimentResult> providingJob) {
        addRequiredJob(providingJob);
        inputProvidingJobs.add(providingJob);
    }

    protected List<ExperimentResult> awaitInputs() {
        //It is important that we skip failing jobs here and remove the instances from the analysis.

        if (successfulInstances != null)
            return successfulInstances;

        failedInstances = new ArrayList<>();
        successfulInstances = inputProvidingJobs.stream().map(job -> {
            try {
                return job.awaitResult();
            } catch (ExecutionException e) {
                LOG().error("Dependent Instance Job failed: " + job.getClass().getSimpleName(), e);
                failedInstances.add(job);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return successfulInstances;
    }

    public List<JJob<ExperimentResult>> getFailedInstances() {
        return failedInstances;
    }

    public boolean hasFailedInstances() {
        return failedInstances != null && !failedInstances.isEmpty();
    }

    @FunctionalInterface
    public interface Factory<T extends DataSetJob> {
        default T createToolJob(Iterable<JJob<ExperimentResult>> dataSet) {
            final T job = makeJob();
            job.addInputProvidingJobs(dataSet);
            return job;
        }

        T makeJob();
    }


}
