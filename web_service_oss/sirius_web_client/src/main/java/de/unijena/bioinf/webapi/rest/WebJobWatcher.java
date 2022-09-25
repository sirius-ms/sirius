/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.webapi.rest;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.NetUtils;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.WaiterJJob;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.JobId;
import de.unijena.bioinf.ms.rest.model.JobInputs;
import de.unijena.bioinf.ms.rest.model.JobTable;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

final class WebJobWatcher { //todo rename to RestJobWatcher
    private static final int INIT_WAIT_TIME = 50;
    private static final int STAY_AT_INIT_TIME = 3;

    private final Map<JobId, RestWebJJob<?, ?, ?>> waitingJobs = new ConcurrentHashMap<>();
    private final Set<SubmissionWaiterJJob<?, ?, ?>> jobsToSubmit = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final RestAPI api;
    private WebJobWatcherJJob watcherJob = null;
    private final Lock watcherJobLock = new ReentrantLock();
    private WebJobSubmitterJJob submitterJob = null;
    private final Lock submitterJobLock = new ReentrantLock();

    private final AtomicBoolean isShutDown = new AtomicBoolean(false);

    //this is for efficient job update even with a large number of jobs on large multi core machines
    public WebJobWatcher(RestAPI api) {
        this.api = api;
    }


    public <I, O, R> SubmissionWaiterJJob<I, O, R> submitAndWatchJob(@NotNull final I jobInput, JobTable type, BiFunction<I, JobId, RestWebJJob<I, O, R>> jobBuilder) throws IOException {
        checkWatcherJob();
        checkSubmitterJob();

        final boolean notIfy = jobsToSubmit.isEmpty();

        SubmissionWaiterJJob<I, O, R> waiter = new SubmissionWaiterJJob<>(type, jobInput, jobBuilder);
        jobsToSubmit.add(waiter);

        if (notIfy) {
            synchronized (jobsToSubmit) {
//                System.out.println("Wake up submitter!");
                jobsToSubmit.notifyAll();
            }
        }


        return waiter;
    }

    public void shutdown() {
        isShutDown.set(true);
        if (submitterJob != null)
            submitterJob.cancel();
        if (watcherJob != null)
            watcherJob.cancel();
    }

    public void awaitShutdown() {
        shutdown();

        if (submitterJob != null) {
            try {
                submitterJob.awaitResult();
            } catch (ExecutionException e) {
                LoggerFactory.getLogger(getClass()).warn("Error when cancelling SubmitterSJob!", e);
            }
        }

        if (watcherJob != null) {
            try {
                watcherJob.awaitResult();
            } catch (ExecutionException e) {
                LoggerFactory.getLogger(getClass()).warn("Error when cancelling WatcherJob!", e);
            }
        }
    }


    private void checkWatcherJob() {
        if (isShutDown.get())
            throw new IllegalStateException("Watcher is already shut Down! Pls create a new Instance!");
        if (watcherJob == null || watcherJob.isFinished()) {
            watcherJobLock.lock();
            try {
                if (watcherJob == null || watcherJob.isFinished())
                    watcherJob = SiriusJobs.getGlobalJobManager().submitJob(new WebJobWatcherJJob());
            } finally {
                watcherJobLock.unlock();
            }
        }
    }

    private void checkSubmitterJob() {
        if (isShutDown.get())
            throw new IllegalStateException("Job watcher is already shut Down! Pls create a new Instance!");
        if (submitterJob == null || submitterJob.isFinished()) {
            submitterJobLock.lock();
            try {
                if (submitterJob == null || submitterJob.isFinished())
                    submitterJob = SiriusJobs.getGlobalJobManager().submitJob(new WebJobSubmitterJJob());
            } finally {
                submitterJobLock.unlock();
            }
        }
    }

    final class SubmissionWaiterJJob<I, O, R> extends WaiterJJob<RestWebJJob<I, O, R>> {
        private final JobTable table;
        private final I input;
        private final BiFunction<I, JobId, RestWebJJob<I, O, R>> jobBuilder;

        public SubmissionWaiterJJob(JobTable table, I jobInput, BiFunction<I, JobId, RestWebJJob<I, O, R>> jobBuilder) {
            this.table = table;
            this.input = jobInput;
            this.jobBuilder = jobBuilder;
        }

        public void finish(JobId id) {
            super.finish(jobBuilder.apply(input, id));
        }

        void addToMap(@NotNull final Map<JobTable, List<? extends SubmissionWaiterJJob<?, ?, ?>>> jobMap) {
            ((List<SubmissionWaiterJJob<I, O, R>>) jobMap.computeIfAbsent(table,
                    t -> new ArrayList<SubmissionWaiterJJob<I, O, R>>())).add(this);
        }
    }

    final class WebJobSubmitterJJob extends BasicJJob<Boolean> {
        public WebJobSubmitterJJob() {
            super(JobType.WEBSERVICE);
        }

        @Override
        protected Boolean compute() throws Exception {
            long lastSubmission = 0;
            long waitTime;
            checkForInterruption();
            while (!isShutDown.get()) {
                if (jobsToSubmit.isEmpty()) {
                    synchronized (jobsToSubmit) {
                        if (jobsToSubmit.isEmpty()) {
                            System.out.println("Start waiting in Submitter!");
                            jobsToSubmit.wait();
                            System.out.println("Stop waiting in Submitter!");
                        }
                    }
                } else if ((waitTime = (System.currentTimeMillis() - lastSubmission)) < INIT_WAIT_TIME) {
                    System.out.println("====> Sleeping in Submitter: " + (INIT_WAIT_TIME - waitTime));
                    NetUtils.sleep(this::checkForInterruption, INIT_WAIT_TIME - waitTime);
                }

                checkForInterruption();
                if (!jobsToSubmit.isEmpty()) {
                    final JobInputs jobSubmission = new JobInputs();
                    final Map<JobTable, List<? extends SubmissionWaiterJJob<?, ?, ?>>> subWaiterJobs = new HashMap<>();
                    final List<SubmissionWaiterJJob<?, ?, ?>> js = new ArrayList<>(jobsToSubmit);
                    js.forEach(jobsToSubmit::remove);
                    js.forEach(s -> {
                        jobSubmission.addJobInput(s.input, s.table);
                        s.addToMap(subWaiterJobs);
                    });

                    checkForInterruption();

                    // submission in sync with waitingJobs map
                    synchronized (waitingJobs) {
                        if (jobSubmission.hasJobs()) {
                            System.out.println("Start submitting jobs to server: " + jobSubmission.size());
                            StopWatch w = new StopWatch();
                            w.start();
                            EnumMap<JobTable, List<JobUpdate<?>>> submittedJobs = NetUtils.tryAndWait(() -> api.submitJobs(jobSubmission), this::checkForInterruption);
                            submittedJobs.forEach((t, wss) -> {
                                Iterator<? extends SubmissionWaiterJJob<?, ?, ?>> it = subWaiterJobs.get(t).iterator();
                                wss.forEach(j -> {
                                    SubmissionWaiterJJob<?, ?, ?> wj = it.next();
                                    wj.finish(j.getID());
                                    waitingJobs.put(j.getID(), wj.result());
                                });
                            });
//                            System.out.println("Wake up Watcher!");
                            lastSubmission = System.currentTimeMillis();
                            waitingJobs.notifyAll();
                            System.out.println("Submitting jobs to server in: " + w);
                        }
                    }
                }
                checkForInterruption();
            }
            return true;
        }
    }

    final class WebJobWatcherJJob extends BasicJJob<Boolean> {

        public WebJobWatcherJJob() {
            super(JobType.WEBSERVICE);
        }

        @Override
        protected Boolean compute() throws Exception {
            try {
                long waitTime = INIT_WAIT_TIME;
                long emptyIterations = 0;
                while (!isShutDown.get()) {
                    checkForInterruption();

                    if (waitingJobs.isEmpty()) {
                        synchronized (waitingJobs) {
                            if (waitingJobs.isEmpty()) {
                                System.out.println("Start waiting in Watcher!");
                                waitingJobs.wait();
                                System.out.println("Stop waiting in Watcher!");
                            }
                        }
                    }

                    final Set<JobId> toRemove = new HashSet<>();
                    {
                        final Map<JobId, Integer> countingHashes = new HashMap<>();

                        final Map<JobId, RestWebJJob<?, ?, ?>> waitingJobsSnap = new HashMap<>();
                        final List<JobUpdate<?>> finished;

                        synchronized (waitingJobs) {
                            finished = NetUtils.tryAndWait(
                                    () -> api.getFinishedJobs(waitingJobs.keySet().stream().map(id -> id.jobTable).collect(Collectors.toSet()))
                                            .values().stream().flatMap(Collection::stream).collect(Collectors.toCollection(LinkedList::new)),
                                    this::checkForInterruption
                            );

                            finished.forEach(j -> {
                                JobId id = j.getGlobalId();
                                waitingJobsSnap.put(id, waitingJobs.get(id));
                            });
                        }

                        checkForInterruption();

//                        System.out.println("Number of jobs retrieved: " + finished.size());

                        try {
                            if (!finished.isEmpty()) {
                                //update, find orphans and notify finished jobs
                                for (JobUpdate<?> up : finished) {
                                    checkForInterruption();
                                    try {
                                        final JobId gid = up.getGlobalId();
                                        RestWebJJob<?, ?, ?> job = waitingJobsSnap.get(gid);
                                        if (job == null) {
                                            logWarn("Job \"" + up.getGlobalId().toString() + "\" was found on the server but is unknown locally. Deleting it to prevent dangling jobs!");
                                            toRemove.add(up.getGlobalId());
                                        } else {
                                            job.getJobCountingHash().ifPresent(h -> countingHashes.put(gid, h));
                                            job.update(up);
                                            toRemove.add(job.getJobId());
                                        }
                                    } catch (Exception e) {
                                        logWarn("Could not update Job", e);
                                    }
                                }
//                                System.out.println("Number of finished jobs to remove: " + toRemove.size());
                            }
                            checkForInterruption();

                            //add canceled jobs to removal

                            final int finishedOnly = toRemove.size();
                            waitingJobs.forEach((k, v) -> {
                                if (v.isFinished()) {
                                    toRemove.add(k);
//                                    System.out.println("Job '" + v.getJobId() + "' with state '" +v.getState()+ "' has been registered for deletion from server!");
                                }
                            });
                            System.out.println("Number of finished jobs to remove (finished/finished+canceled): " + finishedOnly + "/" + toRemove.size());
                        } finally {
                            if (!toRemove.isEmpty()) {
                                // not in sync because it may take some time and is not needed since jobwatcher is singlethreaded
                                NetUtils.tryAndWait(() -> api.deleteJobs(toRemove, countingHashes), this::checkForInterruption);
                                toRemove.forEach(waitingJobs::remove);
                            }
                        }
                    }
//                    System.out.println("Step4: Delete took: " + watch);
                    checkForInterruption();
                    // if nothing was finished increase waiting time
                    // else set back to normal for fast reaction times
                    if (toRemove.isEmpty()) {
                        if (++emptyIterations > STAY_AT_INIT_TIME)
                            waitTime = (long) Math.min(waitTime * NetUtils.WAIT_TIME_MULTIPLIER, 1000);
                        logInfo("No prediction jobs finished. Try again in " + waitTime / 1000d + "s");
                    } else {
                        emptyIterations = 0;
                        waitTime = INIT_WAIT_TIME;
                    }

                    NetUtils.sleep(this::checkForInterruption, waitTime);
//                    System.out.println("Step5: Full iteration took: " + watch);
                }
            } catch (
                    InterruptedException e) {
                if (isShutDown.get())
                    return true;
                throw e;
            }
            return true;
        }

        @Override
        protected synchronized void cleanup() {
            super.cleanup();

            logDebug("Canceling WebWaiterJobs");
            synchronized (waitingJobs) {
                try {
                    waitingJobs.values().forEach(WaiterJJob::cancel); //this jobs are not submitted to the job manager and need no be canceled manually
                    logDebug("Try to delete leftover jobs on web server...");
                    NetUtils.tryAndWait(() -> api.deleteJobs(waitingJobs.keySet(), Collections.emptyMap()), () -> {
                    }, 4000);
                    logDebug("Job deletion Done!");
                } catch (InterruptedException | TimeoutException e) {
                    logWarn("Failed to delete remote jobs from server!", e);
                } finally {
                    waitingJobs.clear();
                }
            }
        }
    }
}
