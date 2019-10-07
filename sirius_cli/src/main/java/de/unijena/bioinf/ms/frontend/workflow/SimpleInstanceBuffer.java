package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.subtools.Instance;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;

import de.unijena.bioinf.projectspace.CompoundContainerId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleInstanceBuffer implements InstanceBuffer {
    private final Iterator<Instance> instances;
    private final List<InstanceJob.Factory> tasks;
    private final DataSetJob dependJob;

    private final Set<InstanceJobCollectorJob> runningInstances = new LinkedHashSet<>();

    final Lock lock = new ReentrantLock();
    final Condition isFull = lock.newCondition();
    private final int bufferSize;
    private final AtomicBoolean isCanceled = new AtomicBoolean(false);

    public SimpleInstanceBuffer(int bufferSize, @NotNull Iterator<Instance> instances, @NotNull List<InstanceJob.Factory> tasks, @Nullable DataSetJob dependJob) {
        this.bufferSize = bufferSize;
        this.instances = instances;
        this.tasks = tasks;
        this.dependJob = dependJob;
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
                final Instance instance = instances.next();
                JJob<Instance> jobToWaitOn = (DymmyExpResultJob) () -> instance;
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
                if (it.getState().equals(JJob.JobState.CANCELED))
                    LoggerFactory.getLogger(getClass()).warn("ToolChain Job '" + it.identifier() + "' was canceled on Instance '" + it.instance.getID() + "'");

                if (it.getState().equals(JJob.JobState.FAILED))
                    LoggerFactory.getLogger(getClass()).error("ToolChain Job '" + it.identifier() + "' FAILED on Instance '" + it.instance.getID() + "'", e);

                LoggerFactory.getLogger(getClass()).debug("ToolChain Job '" + it.identifier() + "' finished with state '" + it.getState() + "' on instance '" + it.instance.getID() + "'", e);
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

    private class InstanceJobCollectorJob extends BasicDependentJJob<CompoundContainerId> {
        final Instance instance;

        public InstanceJobCollectorJob(Instance instance) {
            super(JobType.SCHEDULER,ReqJobFailBehaviour.IGNORE); //we want to ignore failing because we do not want to multiply exceptions
            this.instance = instance;
        }


        @Override
        protected CompoundContainerId compute() throws Exception {
            //this runs if all jobs of the instance are finished
            return instance.getID();
        }

        @Override
        public void handleFinishedRequiredJob(JJob required) {
            //we have to run this here to ensure that it is executed even if the required job failed
            lock.lock();
            try {
                runningInstances.remove(this);
                isFull.signal(); //all not needed?
            } finally {
                lock.unlock();
            }
        }
    }
}
