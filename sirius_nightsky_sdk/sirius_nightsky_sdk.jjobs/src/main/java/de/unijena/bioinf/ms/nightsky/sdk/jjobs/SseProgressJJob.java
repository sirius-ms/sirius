/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */


package de.unijena.bioinf.ms.nightsky.sdk.jjobs;

import de.unijena.bioinf.jjobs.WaiterJJob;
import de.unijena.bioinf.ms.nightsky.sdk.NightSkyClient;
import de.unijena.bioinf.ms.nightsky.sdk.model.Job;
import de.unijena.bioinf.ms.nightsky.sdk.model.JobOptField;
import de.unijena.bioinf.ms.nightsky.sdk.model.JobProgress;
import de.unijena.bioinf.sse.DataObjectEvent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Flow;

/**
 * This is an adapter class that allow to control and monitor a NighSky-API-Job via JJobs infrastructure
 */
public class SseProgressJJob extends WaiterJJob<Job> {
    private final NightSkyClient siriusClient;
    private final String jobId;
    private final String projectId;

    private Flow.Subscriber<DataObjectEvent<?>> subscriber;

    public SseProgressJJob(NightSkyClient siriusClient, String projectId, String jobId) {
        this(siriusClient, projectId, jobId, JobState.SUBMITTED);
    }

    public SseProgressJJob(NightSkyClient siriusClient, String projectId, Job job) {
        this(siriusClient, projectId, job.getId(),
                job.getProgress() != null
                        ? JobState.valueOf(job.getProgress().getState().name())
                        : JobState.SUBMITTED
        );
    }

    public SseProgressJJob(NightSkyClient siriusClient, String projectId, String jobId, JobState state) {
        super();
        this.siriusClient = siriusClient;
        this.jobId = jobId;
        this.projectId = projectId;
        setState(state);

        subscriber = new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                Job j = siriusClient.jobs().getJob(projectId, jobId, List.of(JobOptField.PROGRESS));
                if (updateAndCheckIfDone(j))
                    siriusClient.removeEventListener(this);
            }

            @Override
            public void onNext(DataObjectEvent<?> event) {
                Job j = (Job) event.getData();
                if (updateAndCheckIfDone(j))
                    siriusClient.removeEventListener(this);
            }

            @Override
            public void onError(Throwable throwable) {
                if (throwable instanceof Exception ex)
                    crash(ex);
                else
                    crash(new RuntimeException(throwable));
                siriusClient.removeEventListener(this);
            }

            @Override
            public void onComplete() {
                logWarn("Canceled Sse waiter job running but sse connection has been closed");
            }
        };

        siriusClient.addJobEventListener(subscriber, jobId, projectId);
    }

    @Override
    public void cancel(boolean mayInterruptIfRunning) {
        // should not be synchronized since it call blocking http req
        // just initiate cancelling process in backend. But do not wait for it.
        // Callback via sse event will finally stop the job and notify listeners about cancellation
        siriusClient.jobs().deleteJob(projectId, jobId, true, false);
    }

    private boolean updateAndCheckIfDone(Job wrappedJob) {
        if (wrappedJob == null) {
            crash(new IllegalStateException("Nighsky API Job with ID '" + projectId + "." + jobId + "' does not exist!"));
            return true;
        }

        JobProgress p = wrappedJob.getProgress();

        if (p == null) {
            crash(new IllegalStateException("Got Job update event without progress info. Cannot update jobs state. This is likely to be a bug! Stopping to prevent running forever: " + projectId + "." + jobId));
            return true;
        }

        if (Optional.ofNullable(p.isIndeterminate())
                .orElse(false) || p.getMaxProgress() == null || p.getCurrentProgress() == null) {
            updateProgress(0, 1, p.getMessage());
        } else {
            updateProgress(p.getMaxProgress(), p.getCurrentProgress(), p.getMessage());
        }

        setState(JobState.valueOf(p.getState().name()));

        if (p.getState() == JobProgress.StateEnum.FAILED) {
            crash(new Exception(p.getErrorMessage()));
            return true;
        }

        if (p.getState() == JobProgress.StateEnum.CANCELED) {
            super.cancel(true);
            return true;
        }

        if (p.getState() == JobProgress.StateEnum.DONE) {
            finish(wrappedJob);
            return true;
        }
        return false;
    }
}
