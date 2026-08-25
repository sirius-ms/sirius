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

package io.sirius.ms.sdk.jjobs;

import de.unijena.bioinf.jjobs.JJob;
import io.sirius.ms.sdk.SiriusClient;
import io.sirius.ms.sdk.api.JobsApiCompat;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobProgress;
import io.sirius.ms.sdk.model.JobState;
import io.sirius.ms.sse.DataEventType;
import io.sirius.ms.sse.DataObjectEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The waiter's terminal state must be driven by {@code finish}, {@code crash} and {@code cancel}.
 * Terminal states are final in jjobs, so mirroring the server state must never move the waiter into
 * a terminal state on its own - the result or the exception would silently be dropped.
 */
public class SseProgressJJobTest {
    private static final String PROJECT_ID = "test-project";
    private static final String JOB_ID = "42";

    private SiriusClient client;

    @BeforeEach
    public void setUp() {
        client = mock(SiriusClient.class);
        when(client.jobs()).thenReturn(mock(JobsApiCompat.class));
    }

    @Test
    public void doneServerJobIsReportedAsResult() throws ExecutionException {
        SseProgressJJob job = new SseProgressJJob(client, PROJECT_ID, JOB_ID);
        Job done = serverJob(JobState.DONE, null);

        pushEvent(done);

        assertSame(done, job.awaitResult(), "result of a DONE server job must be handed to the waiter");
        assertEquals(JJob.JobState.DONE, job.getState());
    }

    @Test
    public void failedServerJobIsReportedAsException() {
        SseProgressJJob job = new SseProgressJJob(client, PROJECT_ID, JOB_ID);

        pushEvent(serverJob(JobState.FAILED, "backend blew up"));

        ExecutionException e = assertThrows(ExecutionException.class, job::awaitResult);
        assertEquals("backend blew up", e.getCause().getMessage());
        assertEquals(JJob.JobState.FAILED, job.getState());
    }

    @Test
    public void canceledServerJobIsReportedAsCancellation() {
        SseProgressJJob job = new SseProgressJJob(client, PROJECT_ID, JOB_ID);

        pushEvent(serverJob(JobState.CANCELED, null));

        assertThrows(ExecutionException.class, job::awaitResult);
        assertEquals(JJob.JobState.CANCELED, job.getState());
    }

    @Test
    public void nonTerminalServerStateIsMirroredWithoutCompletingTheJob() {
        SseProgressJJob job = new SseProgressJJob(client, PROJECT_ID, JOB_ID);

        pushEvent(serverJob(JobState.RUNNING, null));

        assertEquals(JJob.JobState.RUNNING, job.getState());
    }

    private Job serverJob(JobState state, String errorMessage) {
        JobProgress progress = new JobProgress()
                .state(state)
                .indeterminate(false)
                .currentProgress(1L)
                .maxProgress(1L)
                .message("progress of " + state)
                .errorMessage(errorMessage);
        return new Job().id(JOB_ID).progress(progress);
    }

    @SuppressWarnings("unchecked")
    private void pushEvent(Job serverJob) {
        ArgumentCaptor<Flow.Subscriber<DataObjectEvent<?>>> subscriber = ArgumentCaptor.forClass(Flow.Subscriber.class);
        verify(client).addJobEventListener(subscriber.capture(), eq(JOB_ID), any());
        subscriber.getValue().onNext(new DataObjectEvent<>(DataEventType.JOB, serverJob, null));
    }
}
