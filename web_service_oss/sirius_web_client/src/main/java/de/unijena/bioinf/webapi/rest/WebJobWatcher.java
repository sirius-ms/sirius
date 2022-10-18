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
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.WaiterJJob;
import de.unijena.bioinf.ms.rest.model.*;
import de.unijena.bioinf.rest.NetUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

final class WebJobWatcher { //todo rename to RestJobWatcher
    private static final int INIT_WAIT_TIME = 100;
    private static final int STAY_AT_INIT_TIME = 3;

    public static final String JOB_WATCHER_CLIENT_ID = "JOB_WATCHER";
    public static final String JOB_SUBMITTER_CLIENT_ID = "JOB_SUBMITTER";

    private static List<JobState> RUNNING_AND_FINISHED = List.of(JobState.SUBMITTED, JobState.DONE, JobState.CRASHED, JobState.CANCELED);
    private static List<JobState> FINISHED = List.of(JobState.DONE, JobState.CRASHED, JobState.CANCELED);

    private final Map<JobId, RestWebJJob<?, ?, ?>> waitingJobs = new ConcurrentHashMap<>();
    private final Set<SubmissionWaiterJJob<?, ?, ?>> jobsToSubmit = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final RestAPI api;
    private WebJobWatcherJJob watcherJob = null;
    private final Lock watcherJobLock = new ReentrantLock();
    private WebJobSubmitterJJob submitterJob = null;
    private final Lock submitterJobLock = new ReentrantLock();

    private final Lock submissionLock = new ReentrantLock();


    private final AtomicBoolean isShutDown = new AtomicBoolean(false);

    //this is for efficient job update even with a large number of jobs on large multi core machines
    public WebJobWatcher(RestAPI api) {
        this.api = api;
    }


    public <I, O, R> SubmissionWaiterJJob<I, O, R> submitAndWatchJob(@NotNull final I jobInput, JobTable type, BiFunction<I, JobId, RestWebJJob<I, O, R>> jobBuilder) throws IOException {
//        System.out.println("AddJob: Adding job!");
        SubmissionWaiterJJob<I, O, R> waiter = new SubmissionWaiterJJob<>(type, jobInput, jobBuilder);
        jobsToSubmit.add(waiter);
//        System.out.println("AddJob: Adding job DONE!");

        checkSubmitterJob();

        //notifying is not an issue since non empty break is not notified like this.
        synchronized (jobsToSubmit) {
//            System.out.println("AddJob :Wake up submitter!");
            jobsToSubmit.notifyAll();
//            System.out.println("AddJob :Wake up submitter DONE!");
        }

        checkWatcherJob();
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
        if (isShutDown.get()){
            LoggerFactory.getLogger(getClass()).warn("Watcher is already shut Down! Pls create a new Instance!");
            return;
        }
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
        if (isShutDown.get()){
            LoggerFactory.getLogger(getClass()).warn("Watcher is already shut Down! Pls create a new Instance!");
            return;
        }
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

        public synchronized void finishAndAddToWaitingJobs(JobId id) {
            final RestWebJJob<I, O, R> j = jobBuilder.apply(input, id);
            waitingJobs.put(id, j);
            finish(j);
        }
    }

    final class WebJobSubmitterJJob extends BasicJJob<Boolean> {
        public WebJobSubmitterJJob() {
            super(JobType.WEBSERVICE);
        }

        @Override
        protected Boolean compute() throws Exception {
            AtomicLong lastSubmission = new AtomicLong(0);
            long waitTime;
            checkForInterruption();
            while (!isShutDown.get()) {
                try {
                    if (jobsToSubmit.isEmpty()) {
                        while (jobsToSubmit.isEmpty()) {
                            synchronized (jobsToSubmit) {
                                if (jobsToSubmit.isEmpty()) { //while and timeout for self healing
//                                    System.out.println("JobSubmitter: WEB: Start waiting in Submitter!");
                                    jobsToSubmit.wait(10000);
//                                    System.out.println("JobSubmitter: WEB: Stop waiting in Submitter!");
                                }
                            }
                        }
                    } else if ((waitTime = (System.currentTimeMillis() - lastSubmission.get())) < INIT_WAIT_TIME) {
//                        System.out.println("JobSubmitter: ====> Sleeping in Submitter: " + (INIT_WAIT_TIME - waitTime));
                        NetUtils.sleepNoRegistration(this::checkForInterruption, INIT_WAIT_TIME - waitTime);
//                        Thread.sleep(INIT_WAIT_TIME - waitTime);
//                        Thread.currentThread().sleep(INIT_WAIT_TIME - waitTime);
                    }

                    checkForInterruption();
                    boolean notify = NetUtils.tryAndWait(() -> {
                        if (!jobsToSubmit.isEmpty()) {
                            JobInputs jobSubmission = new JobInputs();
                            final Map<JobTable, List<? extends SubmissionWaiterJJob<?, ?, ?>>> subWaiterJobs = new HashMap<>();
                            final List<SubmissionWaiterJJob<?, ?, ?>> js = new ArrayList<>(jobsToSubmit);
                            for (SubmissionWaiterJJob<?, ?, ?> s : js) {
                                jobSubmission.addJobInput(s.input, s.table);
                                ((List<SubmissionWaiterJJob<?, ?, ?>>) subWaiterJobs.computeIfAbsent(s.table, t -> new ArrayList<>())).add(s);
                            }

                            // submission in sync with waitingJobs map
                            if (jobSubmission.hasJobs()) {
                                submissionLock.lock();
                                try {
//                                    UUID uuid = UUID.randomUUID();
//                                    System.out.println("JobSubmitter: Start submitting jobs to server: " + jobSubmission.size() + " | " + uuid);
//                                    StopWatch w = new StopWatch();
//                                    w.start();


                                    //submit jobs
//                                    System.out.println("JobSubmitter: WEB: submit to server start." + " | " + uuid);

                                    final EnumMap<JobTable, List<JobUpdate<?>>> submittedJobs = api.submitJobs(jobSubmission);

//                                    System.out.println("JobSubmitter: WEB: submit to server DONE." + " | " + uuid);
                                    //add submitted jobs to waiting jobs

                                    submittedJobs.forEach((t, wss) -> {
                                        Iterator<? extends SubmissionWaiterJJob<?, ?, ?>> it = subWaiterJobs.get(t).iterator();
                                        wss.forEach(j -> {
                                            SubmissionWaiterJJob<?, ?, ?> wj = it.next();
                                            wj.finishAndAddToWaitingJobs(j.getID());
                                        });
                                    });

//                                    System.out.println("JobSubmitter: LOCAL: add to watchlist DONE." + " | " + uuid);


                                    js.forEach(jobsToSubmit::remove);
                                    lastSubmission.set(System.currentTimeMillis());
//                                    System.out.println("JobSubmitter: LOCAL: remove from submit list DONE." + " | " + uuid);
                                } finally {
                                    submissionLock.unlock();
                                }
                            }
                            return jobSubmission.hasJobs();
                        }
                        return false;

                    }, this::checkForInterruption);
                    if (notify) {
                        synchronized (waitingJobs) {
//                            System.out.println("JobSubmitter: Start Wake up Watcher!");
                            waitingJobs.notifyAll();
//                            System.out.println("JobSubmitter: Stop Wake up Watcher!");
                        }
                    }
                } catch (InterruptedException e) {
                    if (isShutDown.get())
                        return true;
                    logWarn("JobSubmitter thread was interrupted unexpectedly. Try to recover!");
                    //todo recovery of lost jobs
                } catch (Exception e) {
                    logError("Unexpected error in JobSubmitter thread. Try to recover!", e);
                }
            }
            logWarn("Close Job Submitter!");
            return true;
        }

        @Override
        protected void cleanup() {
            super.cleanup();
            checkSubmitterJob();
        }
    }

    final class WebJobWatcherJJob extends BasicJJob<Boolean> {

        public WebJobWatcherJJob() {
            super(JobType.WEBSERVICE);
        }

        @Override
        protected Boolean compute() throws Exception {

            long waitTime = INIT_WAIT_TIME;
//            long emptyIterations = 0;
            checkForInterruption();
            while (!isShutDown.get()) {
                try {
                    checkForInterruption();
                    while (waitingJobs.isEmpty()) {
                        synchronized (waitingJobs) {
                            if (waitingJobs.isEmpty()) {
//                                System.out.println("JobWatcher: Start waiting!");
                                waitingJobs.wait(10000);
//                                System.out.println("JobWatcher: Stop waiting!");
                            }
                        }
                    }

                    checkForInterruption();

                    final Set<JobId> toRemove = new HashSet<>();
                    final Set<JobId> toReset = new HashSet<>();
                    final Map<JobId, Integer> countingHashes = new HashMap<>();


                    NetUtils.tryAndWait(() -> {
                        toRemove.clear();
                        toReset.clear();
                        countingHashes.clear();

                        final Map<JobId, RestWebJJob<?, ?, ?>> runningAndFinishedJobs = new HashMap<>();
                        final List<JobUpdate<?>> runningAndFinished;

//                        System.out.println("JobWatcher WEB: Start get jobs");
                        submissionLock.lock();
                        try {
                            runningAndFinished =
                                    api.getJobsByState( //get finished and running jobs
                                            waitingJobs.keySet().stream().map(id -> id.jobTable).collect(Collectors.toSet()), //only request listed jobs
//                                            Set.of(JobTable.JOBS_COVTREE, JobTable.JOBS_CANOPUS, JobTable.JOBS_FINGERID),
                                            RUNNING_AND_FINISHED
                                    ).values().stream().flatMap(Collection::stream).collect(Collectors.toCollection(LinkedList::new));

//                            System.out.println("JobWatcher WEB: Stop get jobs1");
                        } finally {
                            submissionLock.unlock();
                        }
                        runningAndFinished.forEach(j -> {
                            JobId id = j.getGlobalId();
                            runningAndFinishedJobs.put(id, waitingJobs.get(id));
                        });

//                        System.out.println("JobWatcher Number of jobs retrieved: " + runningAndFinished.size());

                        if (!runningAndFinished.isEmpty()) {
                            //update, find orphans and notify finished jobs
                            for (JobUpdate<?> up : runningAndFinished) {
                                try {
                                    final JobId gid = up.getGlobalId();
                                    RestWebJJob<?, ?, ?> job = runningAndFinishedJobs.get(gid);
                                    if (job == null) {
                                        logWarn("Job \"" + up.getGlobalId().toString() + "\" was found on the server but is unknown locally. Deleting it to prevent dangling jobs!");
                                        toRemove.add(up.getGlobalId());
                                    } else {
                                        job.update(up);
                                        if (up.getState() > de.unijena.bioinf.ms.rest.model.JobState.FETCHED.ordinal()) {
                                            job.getJobCountingHash().ifPresent(h -> countingHashes.put(gid, h));
                                            toRemove.add(job.getJobId());
                                        } else if (up.getState() == de.unijena.bioinf.ms.rest.model.JobState.FETCHED.ordinal()) {
                                            if (job.checkRunningTimeout()) {
                                                logWarn("Web Job with Id '" + up.getGlobalId() + "' has been fetched by a worker but takes longer than expected. Maybe the worker died during processing. Try to reset and recompute!");
                                                toReset.add(job.getJobId());
                                                job.reset();
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    logWarn("Could not update Job", e);
                                }
                            }
//                            System.out.println("JobWatcher Number of finished jobs to remove: " + toRemove.size());
                        }
                    }, this::checkForInterruption);

                    // add probably canceled or failed jobs to remove list
                    waitingJobs.forEach((id,job) -> {
                        if (job.isUnSuccessfulFinished())
                            toRemove.add(id);
                    });


                    if (!toRemove.isEmpty()) {
                        // not in sync because it may take some time and is not needed since jobwatcher is singlethreaded
//                        System.out.println("JobWatcher WEB: Start remove jobs: " + toRemove.stream().map(JobId::toString).collect(Collectors.joining(",")));
                        NetUtils.tryAndWait(() -> {
                            api.deleteJobs(toRemove, countingHashes);
                            toRemove.forEach(j -> {
                                RestWebJJob<?, ?, ?> v = waitingJobs.get(j);
//                                if (v == null)
//                                    System.out.println("==> JOB '" + j.toString() + "' to remove does not exist in SIRIUS");
//                                else if (!v.isFinished())
//                                    System.out.println("==> JOB '" + j.toString() + "' to remove is NOT finished!!!!!!!!!!!");
                                waitingJobs.remove(j);
                            });
                        }, this::checkForInterruption);
//                        System.out.println("JobWatcher WEB: End remove jobs: " + toRemove.stream().map(JobId::toString).collect(Collectors.joining(",")));
                    }

                    if (!toReset.isEmpty()) {
//                        System.out.println("JobWatcher WEB: Start reset jobs: " + toReset.stream().map(JobId::toString).collect(Collectors.joining(",")));
                        NetUtils.tryAndWait(() -> api.resetJobs(toReset), this::checkForInterruption);
                        logWarn("Resetting " + toReset.size() + "jobs due to unexpected long computations time!");
//                        System.out.println("WEB: END reset jobs: " + toReset.stream().map(JobId::toString).collect(Collectors.joining(",")));
                    }

//                    }
//                    System.out.println("Step4: Delete took: " + watch);
//                    checkForInterruption();
                    // if nothing was finished increase waiting time
                    // else set back to normal for fast reaction times
                    /*if (toRemove.isEmpty()) {
                        if (++emptyIterations > STAY_AT_INIT_TIME)
                            waitTime = (long) Math.min(waitTime * NetUtils.WAIT_TIME_MULTIPLIER, 1000);
                        logInfo("No prediction jobs finished. Try again in " + waitTime / 1000d + "s");
                    } else {
                        emptyIterations = 0;
                        waitTime = INIT_WAIT_TIME;
//                        logInfo("No prediction jobs finished. Try again in " + waitTime / 1000d + "s");
                    }*/


//                    System.out.println("JobWatcher Start sleep: " + waitTime);
                    NetUtils.sleepNoRegistration(this::checkForInterruption, waitTime);
//                    Thread.currentThread().sleep(waitTime);
//                    System.out.println("JobWatcher Stop sleep: " + waitTime);
//
//
//
//
//                } catch (IOException e) {
//                    if (isShutDown.get())
//                        return true;
//                    logWarn("Error during job watcher: " + e.getMessage() + " | Try reconnect");
//                    System.out.println("Error during job watcher: " + e.getMessage() + " | Try reconnect");
//                    ProxyManager.closeStaleConnections(RestAPI.JOB_WATCHER_CLIENT_ID);
                } catch (InterruptedException e) {
                    if (isShutDown.get())
                        return true;
                    logWarn("JobWatcher thread was interrupted unexpectedly. Try to recover!");
                    //todo recovery of lost jobs?
                } catch (Exception e) {
                    logError("Unexpected error in JobWatcher thread. Try to recover!", e);
                }
            }
            logWarn("Close Job Watcher!");
            return true;
        }

        @Override
        protected void cleanup() {
            super.cleanup();

            logDebug("Canceling WebWaiterJobs");
            try {
                waitingJobs.values().forEach(WaiterJJob::cancel); //this jobs are not submitted to the job manager and need no be canceled manually
                logDebug("Try to delete leftover jobs on web server...");
                NetUtils.tryAndWait(() -> api.deleteJobs(waitingJobs.keySet(), Collections.emptyMap()),
                        this::checkForInterruption, 4000);
                logDebug("Job deletion Done!");
                waitingJobs.clear();
            } catch (InterruptedException | TimeoutException e) {
                logWarn("Failed to delete remote jobs from server!", e);
            } finally {
                checkWatcherJob();
            }
        }
    }
}
