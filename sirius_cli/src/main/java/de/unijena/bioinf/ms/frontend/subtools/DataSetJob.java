package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.frontend.io.projectspace.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class DataSetJob extends BasicDependentJJob<Iterable<Instance>> implements SubToolJob {
    private List<JJob<?>> failedJobs = new ArrayList<>();
    private List<Instance> failedInstances = new ArrayList<>();
    private List<Instance> inputInstances = new ArrayList<>();

    private final Predicate<Instance> inputValidator;

    public DataSetJob(@NotNull Predicate<Instance> inputValidator) {
        this(inputValidator, ReqJobFailBehaviour.WARN);
    }

    public DataSetJob(@NotNull Predicate<Instance> inputValidator, @NotNull ReqJobFailBehaviour failBehaviour) {
        super(JobType.SCHEDULER, failBehaviour);
        this.inputValidator = inputValidator;
    }

    @Override
    protected Iterable<Instance> compute() throws Exception {
        checkInputs();
        computeAndAnnotateResult(inputInstances);
        return inputInstances;
    }

    protected void checkInputs() {
        {
            final Map<Boolean, List<Instance>> splitted = inputInstances.stream().collect(Collectors.partitioningBy(inputValidator));
            inputInstances = splitted.get(true);
            failedInstances = splitted.get(false);
        }

        if (inputInstances == null || inputInstances.isEmpty())
            throw new IllegalArgumentException("No Input found, All dependent SubToolJobs are failed.");
        if (!failedJobs.isEmpty())
            LOG().warn("There are " + failedJobs.size() + " failed input providing InstanceJobs!" + System.lineSeparator()
                    + "Skipping Failed InstanceJobs: " + System.lineSeparator()
                    + failedJobs.stream().map(JJob::identifier).collect(Collectors.joining(System.lineSeparator()))
            );
        if (!failedInstances.isEmpty())
            LOG().warn("There are " + failedInstances.size() + " invalid input Instances!" + System.lineSeparator()
                    + "Skipping Invalid Input Instances: " + System.lineSeparator()
                    + failedInstances.stream().map(Instance::toString).collect(Collectors.joining(System.lineSeparator()))
            );
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        failedJobs = null;
    }

    @Override
    public synchronized void handleFinishedRequiredJob(JJob required) {
        if (required instanceof InstanceJob) {
            final Object r = required.result();
            if (r == null)
                failedJobs.add(required);
            else
                inputInstances.add((Instance) r);
        }
    }


    protected abstract void computeAndAnnotateResult(final @NotNull List<Instance> expRes) throws Exception;

    public List<JJob<?>> getFailedJobs() {
        return failedJobs;
    }

    public boolean hasFailedInstances() {
        return failedJobs != null && !failedJobs.isEmpty();
    }

    @FunctionalInterface
    public interface Factory<T extends DataSetJob> {
        default T createToolJob(Iterable<JJob<Instance>> dataSet) {
            final T job = makeJob();
            dataSet.forEach(job::addRequiredJob);
            return job;
        }

        T makeJob();
    }


}
