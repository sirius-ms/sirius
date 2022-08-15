/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.compute.model;


import de.unijena.bioinf.jjobs.JJob;
import lombok.Getter;
import lombok.Setter;

/**
 * Progress information of a computation job that has already been submitted to SIRIUS.
 * if  currentProgress == maxProgress job is finished and should change to state done soon.
 * if a job is DONE all results can be accessed via the Project-Spaces api.
 */
@Getter
@Setter
public class JobProgress {
    /**
     * Is the progress indeterminate or not
     */
    boolean indeterminate;
    /**
     * Current state of the Jobs in the SIRIUS internal Job scheduler
     *
     *         WAITING: Waiting for submission to ExecutorService (e.g. due to dependent jobs)
     *         READY: Ready for submission but not yet enqueued for submission to ExecutorService.
     *         QUEUED: Enqueued for submission to ExecutorService.
     *         SUBMITTED: Submitted and waiting to be executed.
     *         RUNNING: Job is running.
     *         CANCELED: Jobs is finished due to cancellation by suer or dependent jobs.
     *         FAILED: Job is finished but failed.
     *         DONE: Job finished successfully.
     */
    JJob.JobState state;
    /**
     * Current progress value of the job.
     */
    Long currentProgress;
    /**
     * Progress value to reach (might also change during execution)
     */
    Long maxProgress;
    /**
     * Progress information and warnings.
     */
    String message;
    /**
     * Error message if the job did not finish successfully failed.
     */
    String errorMessage;
}