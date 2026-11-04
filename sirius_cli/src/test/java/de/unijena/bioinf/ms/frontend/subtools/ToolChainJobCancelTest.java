/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2026 Bright Giant GmbH
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

package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A tool job is cancelled without interrupting its own thread - an interrupt closes the project's database
 * channel - while its sub-jobs are cancelled as asked. And a cancelled tool job ends: jjobs counts a job as
 * ended once its cleanup has run, which a cancel that bypasses the library's own skips.
 */
@Timeout(30)
class ToolChainJobCancelTest {

    private JobManager jobs;

    @BeforeEach
    void setUp() {
        jobs = new JobManager(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        jobs.shutdownNow();
    }

    /** Blocks until cancelled, and notes whether its thread was interrupted. */
    private static class SubJob extends BasicJJob<Boolean> {
        final CountDownLatch started = new CountDownLatch(1);
        final AtomicBoolean interrupted = new AtomicBoolean();

        SubJob() {
            super(JobType.CPU);
        }

        @Override
        protected Boolean compute() {
            started.countDown();
            while (getState() != JobState.CANCELED) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    interrupted.set(true);
                }
            }
            return false;
        }
    }

    /** Waits until cancelled, and notes whether its own thread was interrupted. */
    private static class ToolJob extends ToolChainJobImpl<Boolean> {
        final SubJob sub = new SubJob();
        final CountDownLatch subStarted = sub.started;
        final AtomicBoolean interrupted = new AtomicBoolean();

        ToolJob(JobManager jobs) {
            super(jobs);
        }

        @Override
        protected Boolean compute() {
            submitSubJob(sub);
            while (getState() != JobState.CANCELED) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    interrupted.set(true);
                }
            }
            return false;
        }

        @Override
        public boolean isAlreadyComputed(@NotNull Instance inst) {
            return false;
        }

        @Override
        public String getProjectName() {
            return "test";
        }
    }

    @Test
    void aRunningToolJobIsNotInterruptedButItsSubJobsAre() throws Exception {
        ToolJob job = jobs.submitJob(new ToolJob(jobs));
        assertTrue(job.subStarted.await(10, TimeUnit.SECONDS));

        job.cancel(true);
        job.awaitEnd();

        assertEquals(JJob.JobState.CANCELED, job.getState());
        assertFalse(job.interrupted.get(), "the tool job's own thread must not be interrupted");
        job.sub.awaitEnd();
        assertTrue(job.sub.interrupted.get(), "its sub-jobs are cancelled as asked");
    }

    @Test
    void aToolJobCancelledBeforeItWasSubmittedEnds() throws Exception {
        ToolJob job = new ToolJob(jobs);
        job.cancel(true);
        job.awaitEnd(); //returns only once its cleanup has run
        assertEquals(JJob.JobState.CANCELED, job.getState());
    }
}
