/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.*;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.projectspace.Instance;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleInstanceBuffer implements InstanceBuffer, JobSubmitter {
    private final Iterator<? extends Instance> instances;
    private final List<InstanceJob.Factory<?>> tasks;
    private final DataSetJob dependJob;
    protected final JobSubmitter jobSubmitter;

    private final Set<InstanceJobCollectorJob> runningInstances = Collections.newSetFromMap(new ConcurrentHashMap<>());

    final Lock lock = new ReentrantLock();
    final Condition isFull = lock.newCondition();
    private final int bufferSize;
    private final AtomicBoolean isCanceled = new AtomicBoolean(false);

    private final JobProgressMerger progressSupport;
    //stats
    StopWatch w = null;
    AtomicInteger instanceComputed = null;

    public SimpleInstanceBuffer(int bufferSize, @NotNull Iterator<? extends Instance> instances, @NotNull List<InstanceJob.Factory<?>> tasks, @Nullable DataSetJob.Factory<?> dependJobFactory, @NotNull JobProgressMerger progressSupport, JobSubmitter jobSubmitter) {
        this.bufferSize = bufferSize < 1 ? Integer.MAX_VALUE : bufferSize;
        this.jobSubmitter = jobSubmitter;
        this.instances = instances;
        this.tasks = tasks;
        this.progressSupport = progressSupport;
        this.dependJob = dependJobFactory == null ? null : dependJobFactory.makeJob(this);
        if (dependJob != null)
            dependJob.addPropertyChangeListener(this.progressSupport);

    }

    @Override
    public @Nullable DataSetJob getCollectorJob() {
        return dependJob;
    }

    @Override
    public void start(final boolean invalidate) throws InterruptedException {
        int lastCheck = 0;
        instanceComputed = new AtomicInteger(0);
        w = new StopWatch();
        w.start();

        try {
            while (instances.hasNext()) {
                checkForCancellation();

                { // calculate current throughput
                    final int snap = instanceComputed.get();
                    if ((snap - lastCheck) > 10) {
                        System.out.println("########################################");
                        System.out.println("## Computed " + snap + " instances. Current throughput: " + String.format("%,.2f", (snap / (w.getTime() / 1000d / 60d))) + " instances/minute");
                        System.out.println("########################################");
                        lastCheck = snap;
                    }
                }

                { //gc hint
                    if (instanceComputed.get() % bufferSize == 0) {
                        System.gc(); //hint for the gc to collect som trash after computations
                    }
                }

                lock.lock();
                try {
                    //wait for free slot in buffer if it is full
                    while (runningInstances.size() >= bufferSize) {
                        isFull.await();
                        checkForCancellation();
                    }

                    checkForCancellation();

                    final Instance instance = instances.next();
                    final InstanceJobCollectorJob collector = new InstanceJobCollectorJob(instance, invalidate);
                    JJob<Instance> jobToWaitOn = (DymmyExpResultJob) () -> instance;
                    Map<InstanceJob.Factory<?>, JJob<Instance>> createdJobs = new HashMap<>(tasks.size());
                    for (InstanceJob.Factory<?> task : tasks) {
                        if (task.getInputProvidingFactory() != null && createdJobs.containsKey(task.getInputProvidingFactory())) {
                            jobToWaitOn = task.createToolJob(createdJobs.get(task.getInputProvidingFactory()));
                        } else
                            jobToWaitOn = task.createToolJob(jobToWaitOn);

                        createdJobs.put(task, jobToWaitOn);
                        jobToWaitOn.addPropertyChangeListener(progressSupport);
                        submitJob(jobToWaitOn);
                        collector.addRequiredJob(jobToWaitOn);
                    }
                    runningInstances.add(submitJob(collector));

                    checkForCancellation();

                    // add dependency if necessary
                    if (dependJob != null)
                        dependJob.addRequiredJob(jobToWaitOn);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            LoggerFactory.getLogger(getClass()).info("Buffered Job Submission Canceled. Awaiting Cancellation of running Jobs...");
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
                //already logged by collector job
                if (it.getState().equals(JJob.JobState.CANCELED))
                    LoggerFactory.getLogger(getClass()).warn("ToolChain collector Job '" + it.identifier() + "' was canceled on Instance '" + it.instance + "'");

                if (it.getState().equals(JJob.JobState.FAILED))
                    LoggerFactory.getLogger(getClass()).error("ToolChain collector Job '" + it.identifier() + "' FAILED on Instance '" + it.instance + "'", e);

                LoggerFactory.getLogger(getClass()).debug("ToolChain collector Job '" + it.identifier() + "' finished with state '" + it.getState() + "' on instance '" + it.instance + "'", e);
            }
        });

        System.out.println("########################################");
        System.out.println("## Computed " + instanceComputed.get() + " instances in " + w + "(" + String.format("%,.2f", (instanceComputed.get() / (w.getTime() / 1000d / 60d))) + " instances/minute).");
        System.out.println("########################################");
    }

    @Override
    public void cancel() {
        lock.lock();
        try {
            isCanceled.set(true);
            runningInstances.forEach(JJob::cancel);
            if (dependJob != null)
                dependJob.cancel();
            isFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <Job extends JJob<Result>, Result> Job submitJob(Job job) {
        return jobSubmitter.submitJob(job);
    }

    protected void checkForCancellation() throws InterruptedException {
        if (isCanceled.get())
            throw new InterruptedException("Was cancelled by external Thread");
    }

    private class InstanceJobCollectorJob extends BasicDependentJJob<String> {
        private final Instance instance;
        private final boolean invalidate;
        Set<JJob<?>> toWaitOnCleanUp = Collections.newSetFromMap(new ConcurrentHashMap<>());

        @Override
        public void cancel(boolean mayInterruptIfRunning) {
            toWaitOnCleanUp = new HashSet<>();
            final LinkedList<JJob<?>> deps = new LinkedList<>(this.getRequiredJobs());
            while (deps.peekFirst() != null) {
                JJob<?> current = deps.pollFirst();
                if (current instanceof DependentJJob)
                    deps.addAll(((DependentJJob<?>) current).getRequiredJobs());
                toWaitOnCleanUp.add(current);
                current.cancel(mayInterruptIfRunning);
            }
            if (mayInterruptIfRunning)
                logDebug("Prevent hard interrupt in SimpleInstanceBuffer to protect DB channel!");
            super.cancel(false);
        }


        @Override
        protected void cleanup() {
            // this should always run because we ignore failing of reqiured jobs
            //this runs if all jobs of the instance are finished
            lock.lock();
            try {
                runningInstances.remove(this);
                instanceComputed.incrementAndGet();
                isFull.signalAll();
            } finally {
                lock.unlock();
            }

            toWaitOnCleanUp.forEach(j -> {
                try {
                    j.awaitResult();
                } catch (ExecutionException e) {
                    if (j.getState().equals(JJob.JobState.CANCELED)) {
                        j.logWarn("ToolChain Job canceled due to: " + e.getMessage());
                    } else if (j.getState().equals(JJob.JobState.FAILED)) {
                        j.logError("ToolChain Job failed due to: " + e.getMessage());
                        j.logDebug("ToolChain Job failed: ", e);
                    } else {
                        LoggerFactory.getLogger(getClass()).debug("ToolChain Job Exception with state '" + j.getState() + ".", e);
                    }
                }
            });
            super.cleanup();
        }

        public InstanceJobCollectorJob(Instance instance, final boolean invalidate) {
            super(JobType.SCHEDULER, ReqJobFailBehaviour.IGNORE); //we want to ignore failing because we do not want to multiply exceptions
            this.instance = instance;
            this.invalidate = invalidate;
        }


        @Override
        protected String compute() throws InterruptedException {
            //cleanup is not really needed for CLI but for everything on top that might keep instances alive.
            if (invalidate)
                instance.clearCompoundCache();
            checkForInterruption();
            return instance.getId();
        }

        @Override
        public void handleFinishedRequiredJob(JJob required) {
            toWaitOnCleanUp.add(required);
        }
    }

    public static class Factory implements InstanceBufferFactory<SimpleInstanceBuffer> {
        @Override
        public SimpleInstanceBuffer create(int bufferSize, @NotNull Iterator<? extends Instance> instances, @NotNull List<InstanceJob.Factory<?>> tasks, @Nullable DataSetJob.Factory<?> dependJobFactory, @NotNull JobProgressMerger progressSupport) {
            return new SimpleInstanceBuffer(bufferSize, instances, tasks, dependJobFactory, progressSupport, SiriusJobs.getGlobalJobManager());
        }
    }
}
