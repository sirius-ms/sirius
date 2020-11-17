/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.DependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleInstanceBuffer implements InstanceBuffer, JobSubmitter {
    private final Iterator<? extends Instance> instances;
    private final List<InstanceJob.Factory<?>> tasks;
    private final DataSetJob dependJob;
    protected final JobSubmitter jobSubmitter;

    private final Set<InstanceJobCollectorJob> runningInstances = new LinkedHashSet<>();

    final Lock lock = new ReentrantLock();
    final Condition isFull = lock.newCondition();
    private final int bufferSize;
    private final AtomicBoolean isCanceled = new AtomicBoolean(false);

    public SimpleInstanceBuffer(int bufferSize, @NotNull Iterator<? extends Instance> instances, @NotNull List<InstanceJob.Factory<?>> tasks, @Nullable DataSetJob.Factory<?> dependJobFactory, JobSubmitter jobSubmitter) {
        this.bufferSize = bufferSize < 1 ? Integer.MAX_VALUE : bufferSize;
        this.jobSubmitter = jobSubmitter;
        this.instances = instances;
        this.tasks = tasks;
        this.dependJob = dependJobFactory == null ? null : dependJobFactory.makeJob(this);
    }

    @Override
    public @Nullable DataSetJob getCollectorJob() {
        return dependJob;
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
                final InstanceJobCollectorJob collector = new InstanceJobCollectorJob(instance);

                JJob<Instance> jobToWaitOn = (DymmyExpResultJob) () -> instance;
                for (InstanceJob.Factory<?> task : tasks) {
                    jobToWaitOn = task.createToolJob(jobToWaitOn);
                    collector.addRequiredJob(jobToWaitOn);
                    submitJob(jobToWaitOn);
                }

                checkForCancellation();
                runningInstances.add(submitJob(collector));

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
            new ArrayList<>(runningInstances).forEach(JJob::cancel);
            isFull.signal();
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

    private class InstanceJobCollectorJob extends BasicDependentJJob<CompoundContainerId> {
        final Instance instance;

        @Override
        public void cancel(boolean mayInterruptIfRunning) {
            final LinkedList<JJob<?>> deps = new LinkedList<>(this.requiredJobs());
            while (deps.peekFirst() != null){
                JJob<?> current = deps.pollFirst();
                if (current instanceof DependentJJob)
                    deps.addAll(((DependentJJob<?>)current).requiredJobs());
                current.cancel();
            }
            super.cancel(mayInterruptIfRunning);
        }

        public InstanceJobCollectorJob(Instance instance) {
            super(JobType.SCHEDULER,ReqJobFailBehaviour.IGNORE); //we want to ignore failing because we do not want to multiply exceptions
            this.instance = instance;

            addPropertyChangeListener(evt -> {
                if (isFinished()) {
                    lock.lock();
                    try {
                        runningInstances.remove(this);
                        isFull.signal(); //all not needed?
                    } finally {
                        lock.unlock();
                    }
                }
            });


        }


        @Override
        protected CompoundContainerId compute() {
            // this should always run because we ignore failling of reqiured jobs
            //this runs if all jobs of the instance are finished
            return instance.getID();
        }

        @Override
        public void handleFinishedRequiredJob(JJob required) {
//            System.out.println(required.identifier() +" - " + required.getState().name());
        }
    }

    public static class Factory implements InstanceBufferFactory<SimpleInstanceBuffer> {
        @Override
        public SimpleInstanceBuffer create(int bufferSize, @NotNull Iterator<? extends Instance> instances, @NotNull List<InstanceJob.Factory<?>> tasks, @Nullable DataSetJob.Factory<?> dependJobFactory) {
            return new SimpleInstanceBuffer(bufferSize, instances, tasks, dependJobFactory, SiriusJobs.getGlobalJobManager());
        }
    }
}
