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
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.WaiterJJob;
import de.unijena.bioinf.ms.rest.model.*;
import de.unijena.bioinf.rest.NetUtils;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

final class WebJobWatcher { //todo rename to RestJobWatcher
    private static final int INIT_WAIT_TIME = 25;
    private static final int STAY_AT_INIT_TIME = 3;
    private static final int MAX_SUBMIT_BATCH = 240;

    public static final String JOB_WATCHER_CLIENT_ID = "JOB_WATCHER";
    public static final String JOB_SUBMITTER_CLIENT_ID = "JOB_SUBMITTER";

    private static List<JobState> RUNNING_AND_FINISHED = List.of(JobState.SUBMITTED, JobState.DONE, JobState.CRASHED, JobState.CANCELED);
    private static List<JobState> FINISHED = List.of(JobState.DONE, JobState.CRASHED, JobState.CANCELED);

    private final Map<JobId, RestWebJJob<?, ?, ?>> waitingJobs = new ConcurrentHashMap<>();
    private final Deque<Pair<JobTable, RestWebJJob<?, ?, ?>>> jobsToSubmit = new ConcurrentLinkedDeque<>();

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


    public <I, O, R> RestWebJJob<I, O, R> submitAndWatchJob(@NotNull JobTable type, @NotNull RestWebJJob<I, O, R> jobToSubmit) throws IOException {
        if (isShutDown.get())
            throw new IllegalStateException("WebJobWatcher has been shut down. No submissions possible");

        jobsToSubmit.add(Pair.create(type, jobToSubmit));

        checkSubmitterJob();

        //notify waiting submission thead
        synchronized (jobsToSubmit) {
            jobsToSubmit.notifyAll();
        }

        checkWatcherJob();
        return jobToSubmit;
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
        if (isShutDown.get()) {
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
        if (isShutDown.get()) {
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

    final class WebJobSubmitterJJob extends BasicJJob<Boolean> {
        public WebJobSubmitterJJob() {
            super(JobType.TINY_BACKGROUND);
        }

        @Override
        protected Boolean compute() throws Exception {
            final AtomicLong lastSubmission = new AtomicLong(0);
            long waitTime;
            checkForInterruption();
            while (!isShutDown.get()) {
                try {
                    if (jobsToSubmit.isEmpty()) {
                        while (jobsToSubmit.isEmpty()) { //while and timeout for self healing in case wakeup fails
                            synchronized (jobsToSubmit) {
                                if (jobsToSubmit.isEmpty()) {
                                    jobsToSubmit.wait(10000);
                                }
                            }
                        }
                    } else if ((waitTime = (System.currentTimeMillis() - lastSubmission.get())) < INIT_WAIT_TIME) {
                        NetUtils.sleepNoRegistration(this::checkForInterruption, INIT_WAIT_TIME - waitTime);
                    }

                    //nothing really happened so we can interrupt with preserving a clean state
                    checkForInterruption();

                    NetUtils.tryAndWait(() -> {
                        //collect data to submit
                        final JobInputs jobSubmission = new JobInputs();
                        final Map<JobTable, List<RestWebJJob<?, ?, ?>>> subWaiterJobs = new HashMap<>();
                        {
                            final Iterator<Pair<JobTable, RestWebJJob<?, ?, ?>>> jobsIt = jobsToSubmit.iterator();
                            while (jobsIt.hasNext() && jobSubmission.size() <= MAX_SUBMIT_BATCH) {
                                Pair<JobTable, RestWebJJob<?, ?, ?>> j = jobsIt.next();
                                if (j.getSecond().isFinished()) {
                                    System.out.println("removing canceled/finished job instead of submitting it!");
                                    jobsIt.remove();
                                } else {
                                    jobSubmission.addJobInput(j.getSecond().getInput(), j.getFirst());
                                    subWaiterJobs.computeIfAbsent(j.getFirst(), t -> new ArrayList<>()).add(j.getSecond());
                                }
                            }
                        }
//                            checkForInterruption(); //todo enable
                        if (jobSubmission.hasJobs()) {
                            // submission in sync with waitingJobs map
                            synchronized (waitingJobs) {
                                //submit jobs to server
                                final EnumMap<JobTable, List<JobUpdate<?>>> submittedJobs = api.submitJobs(jobSubmission);

                                //add IDs from submission to jobs and add them to waitingJobs map.
                                submittedJobs.forEach((t, wss) -> {
                                    Iterator<RestWebJJob<?, ?, ?>> it = subWaiterJobs.get(t).iterator();
                                    wss.forEach(j -> {
                                        final RestWebJJob<?, ?, ?> restJJ = it.next();
                                        restJJ.submissionAck(j.getID());
                                        waitingJobs.put(j.getID(), restJJ);
                                    });
                                });

                                lastSubmission.set(System.currentTimeMillis());

                                //remove jobs from submission list
                                for (int i = 0; i < jobSubmission.size(); i++)
                                    jobsToSubmit.removeFirst();

                                //notify waiter in case it is sleeping due to epty map
                                waitingJobs.notifyAll();
                            }
                        }
                        return jobSubmission;
                    }, this::checkForInterruption);
                } catch (TimeoutException | InterruptedException e) {
                    logWarn("JobSubmitter thread was interrupted unexpectedly but state should be clean.  Try to recover!");
                } catch (Exception e) {
                    logError("Unexpected error in JobSubmitter thread. State might be unclean. Try Cancelling all Jobs and Restart submitter.", e);
                    cancelAllNonSubmitted();
                    return false;
                }
            }
            logWarn("=====================> Close Job Submitter!");
            return true;
        }

        @Override
        protected void cleanup() {
            super.cleanup();
            try {
                if (isShutDown.get())
                    cancelAllNonSubmitted();
            } finally {
                checkSubmitterJob();
            }
        }

        public void cancelAllNonSubmitted() {
            logDebug("Cancel pre submission jobs...");
            jobsToSubmit.stream().map(Pair::getSecond).forEach(WaiterJJob::cancel); //this jobs are not submitted to the job manager and need no be canceled manually
            try {
                NetUtils.tryAndWait(() -> api.deleteJobs(waitingJobs.keySet(), Collections.emptyMap()),
                        this::checkForInterruption, 25000);
            } catch (InterruptedException | TimeoutException e) {
                logWarn("Failed to delete remote jobs from server!", e);
            }
            logDebug("Cancel  pre submission jobs Done!");
            jobsToSubmit.clear();
            logDebug("Pre submission cleared!");
        }
    }

    final class WebJobWatcherJJob extends BasicJJob<Boolean> {

        public WebJobWatcherJJob() {
            super(JobType.TINY_BACKGROUND);
        }

        @Override
        protected Boolean compute() throws Exception {

            long waitTime = INIT_WAIT_TIME;
            long emptyIterations = 0;

            while (!isShutDown.get()) {
                try {
                    checkForInterruption();
                    while (waitingJobs.isEmpty()) {
                        synchronized (waitingJobs) {
                            if (waitingJobs.isEmpty()) {
                                waitingJobs.wait(10000);
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

                        final List<JobUpdate<?>> runningAndFinishedUpdates;
                        //fetch running and finished jobs from server in sync with submitter.
                        synchronized (waitingJobs) {
                            runningAndFinishedUpdates =
                                    api.getJobsByState( //get finished and running jobs
                                            waitingJobs.keySet().stream().map(id -> id.jobTable).collect(Collectors.toSet()), //only request listed jobs
                                            RUNNING_AND_FINISHED
                                    ).values().stream().flatMap(Collection::stream).collect(Collectors.toCollection(LinkedList::new));

                        }

                        if (!runningAndFinishedUpdates.isEmpty()) {
                            //update, find orphans and notify finished jobs
                            for (JobUpdate<?> up : runningAndFinishedUpdates) {
                                final JobId gid = up.getGlobalId();
                                RestWebJJob<?, ?, ?> job = waitingJobs.get(gid);
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
                            }
                        }
                    }, this::checkForInterruption);

                    // add probably canceled or failed jobs to remove list
                    waitingJobs.forEach((id, job) -> {
                        if (job.isUnSuccessfulFinished()) {
                            logInfo("Registering canceled or failed local job '" + id + "' for removal on server.");
                            toRemove.add(id);
                        }
                    });


                    if (!toRemove.isEmpty()) {
                        // not in sync because it may take some time and is not needed since jobwatcher is singlethreaded
                        NetUtils.tryAndWait(() -> {
                            api.deleteJobs(toRemove, countingHashes);
                            toRemove.forEach(waitingJobs::remove);
                        }, this::checkForInterruption);
                    }

                    if (!toReset.isEmpty()) {
                        NetUtils.tryAndWait(() -> api.resetJobs(toReset), this::checkForInterruption);
                        logWarn("Resetting " + toReset.size() + " jobs due to unexpected long computations time!");
                    }

                    // if nothing was finished increase waiting time
                    // else set back to normal for fast reaction times
                    if (toRemove.isEmpty()) {
                        if (++emptyIterations > STAY_AT_INIT_TIME)
                            waitTime = (long) Math.min(waitTime * NetUtils.WAIT_TIME_MULTIPLIER, 1000);
                        logInfo("No prediction jobs finished. Waiting before retry " + waitTime / 1000d + "s");
                    } else {
                        emptyIterations = 0;
                        waitTime = INIT_WAIT_TIME;
                    }

                } catch (TimeoutException | InterruptedException e) {
                    logWarn("JobWatcher thread was interrupted unexpectedly. State should be clean. Try to recover!");
                } catch (Exception e) {
                    logError("Unexpected error in JobWatcher thread. State might be unclean. Cancelling all running jobs!", e);
                    deleteAllWaiting();
                    return false;
                }
            }
            logWarn("====================> Close Job Watcher!");
            return true;
        }

        private void deleteAllWaiting() {
            logDebug("Canceling WebWaiterJobs");
            waitingJobs.values().forEach(WaiterJJob::cancel);
            logDebug("Try to delete leftover jobs on web server...");
            try {
                NetUtils.tryAndWait(() -> api.deleteJobs(waitingJobs.keySet(), Collections.emptyMap()),
                        this::checkForInterruption, 10000);
                logDebug("Job deletion Done!");
            } catch (InterruptedException | TimeoutException e) {
                logWarn("Failed to delete remote jobs from server!", e);
            }
            waitingJobs.clear();
        }

        @Override
        protected void cleanup() {
            super.cleanup();
            try {
                if (isShutDown.get())
                    deleteAllWaiting();
            } finally {
                checkWatcherJob();
            }
        }
    }
}
