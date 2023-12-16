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
import org.jetbrains.annotations.NotNull;

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
    private Job wrappedJob = null;

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
                synchronized (SseProgressJJob.this) {
                    wrappedJob = siriusClient.jobs().getJob(projectId, jobId, List.of(JobOptField.PROGRESS));
                    update();
                }
            }

            @Override
            public void onNext(DataObjectEvent<?> event) {
                synchronized (SseProgressJJob.this) {
                    wrappedJob = (Job) event.getData();
                    update();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                crash(throwable);
            }

            @Override
            public void onComplete() {
                logWarn("Canceled Sse waiter job due to closed sse connection");
                SseProgressJJob.super.cancel();
            }
        };

        siriusClient.addJobEventListener(subscriber, jobId, projectId);
    }

    @Override
    public synchronized void cancel(boolean mayInterruptIfRunning) {
        siriusClient.jobs().deleteJob(projectId, jobId, true, true);
        super.cancel(mayInterruptIfRunning);
        siriusClient.removeEventListener(subscriber);
    }

    @Override
    public synchronized void crash(@NotNull Throwable e) {
        super.crash(e);
        siriusClient.removeEventListener(subscriber);
    }

    @Override
    public synchronized void finish(@NotNull Job result) {
        super.finish(result);
        siriusClient.removeEventListener(subscriber);
    }

    private void update() {
        if (wrappedJob == null) {
            crash(new IllegalStateException("Nighsky API Job with ID '" + projectId + "." + jobId + "' does not exist!"));
            return;
        }

        JobProgress p = wrappedJob.getProgress();

        if (p == null) {
            logWarn("Got Job update event without progress info. Cannot update jobs state. This is likely to be a bug!");
            return;
        }

        if (Optional.ofNullable(p.isIndeterminate())
                .orElse(false) || p.getMaxProgress() == null || p.getCurrentProgress() == null){
            updateProgress(0, 1, wrappedJob.getId() + "|" + p.getMessage());
        } else {
            updateProgress(p.getMaxProgress(), p.getCurrentProgress(), wrappedJob.getId() + "|" + p.getMessage());
        }

        if (p.getState() == JobProgress.StateEnum.FAILED) {
            crash(new Exception(p.getErrorMessage()));
        } else if (p.getState() == JobProgress.StateEnum.CANCELED) {
            super.cancel();
        } else if (p.getState() == JobProgress.StateEnum.DONE) {
            super.finish(wrappedJob);
        } else {
            setState(JobState.valueOf(p.getState().name()));
        }
    }
}
