package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.ExperimentResult;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class DataSetJob extends BasicDependentJJob<Iterable<ExperimentResult>> {
    private LinkedHashSet<JJob<ExperimentResult>> inputProvidingJobs = new LinkedHashSet<>();

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

    protected List<ExperimentResult> awaitInputs() throws ExecutionException {
        return inputProvidingJobs.stream().map(JJob::takeResult).collect(Collectors.toList());
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
