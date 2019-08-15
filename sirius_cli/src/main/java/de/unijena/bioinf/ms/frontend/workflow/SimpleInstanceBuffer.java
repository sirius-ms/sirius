package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.babelms.projectspace.ExperimentDirectory;
import de.unijena.bioinf.babelms.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleInstanceBuffer implements InstanceBuffer {
    private final Iterator<ExperimentResult> instances;
    private final List<InstanceJob.Factory> tasks;
    private final DataSetJob dependJob;
    private final SiriusProjectSpace projectSpace;

    private final Set<InstanceJobCollectorJob> runningInstances = new LinkedHashSet<>();

    final Lock lock = new ReentrantLock();
    final Condition isFull = lock.newCondition();
    private final int bufferSize;
    private final AtomicBoolean isCanceled = new AtomicBoolean(false);

    public SimpleInstanceBuffer(int bufferSize, @NotNull Iterator<ExperimentResult> instances, @NotNull List<InstanceJob.Factory> tasks, @Nullable DataSetJob dependJob, @NotNull SiriusProjectSpace projectSpace) {
        this.bufferSize = bufferSize;
        this.instances = instances;
        this.tasks = tasks;
        this.dependJob = dependJob;
        this.projectSpace = projectSpace;
    }


    @Override
    public void start() throws InterruptedException {
        while (instances.hasNext()) {
            checkForCancellation();

            lock.lock();
            try {
                //wait for free slot in buffer if it is full
                while (runningInstances.size() >= bufferSize) {
                    isFull.await();
                    checkForCancellation();
                }

                checkForCancellation();
                final ExperimentResult instance = instances.next();
                JJob<ExperimentResult> jobToWaitOn = (DymmyExpResultJob) () -> instance;
                final InstanceJobCollectorJob collector = new InstanceJobCollectorJob(instance);
                for (InstanceJob.Factory task : tasks) {
                    jobToWaitOn = task.createToolJob(jobToWaitOn);
                    collector.addRequiredJob(jobToWaitOn);
                    SiriusJobs.getGlobalJobManager().submitJob(jobToWaitOn);
                }

                checkForCancellation();
                runningInstances.add(SiriusJobs.getGlobalJobManager().submitJob(collector));

                // add dependency if necessary
                if (dependJob != null)
                    dependJob.addRequiredJob(jobToWaitOn);


            } finally {
                lock.unlock();
            }
        }

        // wait for the last submitted jobs
        List<InstanceJobCollectorJob> last;
        lock.lock();
        try {
            last = new ArrayList<>(runningInstances);
        } finally {
            lock.unlock();
        }

        last.forEach(it -> {
            try {
                it.awaitResult();
            } catch (ExecutionException e) {
                LoggerFactory.getLogger(getClass()).error("Error when executing ToolChain Job within Submitter", e);
            }
        });
    }

    @Override
    public void cancel() {
        lock.lock();
        try {
            isCanceled.set(true);
            isFull.signal();
        } finally {
            lock.unlock();
        }
    }

    protected void checkForCancellation() throws InterruptedException {
        if (isCanceled.get())
            throw new InterruptedException("Was cancelled by external Thread");
    }

    private class InstanceJobCollectorJob extends BasicDependentJJob<ExperimentDirectory> {
        final ExperimentResult instance;

        public InstanceJobCollectorJob(ExperimentResult instance) {
            super(JobType.SCHEDULER);
            this.instance = instance;
        }


        @Override
        protected ExperimentDirectory compute() throws Exception {
            //this runs if all jobs of the instance are finished
            checkForInterruption();
            lock.lock();
            try {
                runningInstances.remove(this);
                isFull.signal(); //all not needed?
                return instance.getAnnotationOrThrow(ExperimentDirectory.class);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void handleFinishedRequiredJob(JJob required) {
            // write results to project space.
            try {
                projectSpace.writeExperiment(instance);
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).error("Could not write results of Job with Name:" + required.LOG().getName() + ", Type: " + required.getClass().getSimpleName() + " to Project-Space", e);
            }
        }
    }
}
