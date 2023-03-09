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

package de.unijena.bioinf.ms.webapi;

import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.jjobs.InputJJob;
import de.unijena.bioinf.jjobs.WaiterJJob;
import de.unijena.bioinf.jjobs.exceptions.TimeoutException;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;

public class WebJJob<I, O, R, ID> extends WaiterJJob<R> implements InputJJob<I, R> {
    public static long WEB_API_JOB_TIME_OUT = PropertyManager.getLong("de.unijena.bioinf.fingerid.web.job.timeout", 1000L * 60L * 60L); //default 1h
    public static long WEB_API_RUNNING_JOB_TIME_OUT = PropertyManager.getLong("de.unijena.bioinf.fingerid.web.job.timeout.running", 1000L * 60L * 5L); //default 5min

    protected I input;
    protected volatile ID jobId;
    protected final IOFunctions.IOFunction<O, R> outputConverter;

    protected volatile de.unijena.bioinf.ms.rest.model.JobState serverState;
    protected volatile long submissionTime;
    protected volatile String errorMessage;

    @Nullable
    public Integer countingHash;

    protected Long runningSince;

    public WebJJob(@Nullable ID jobId, @NotNull IOFunctions.IOFunction<O, R> outputConverter) {
        this(jobId, null, outputConverter);
    }

    public WebJJob(@Nullable ID jobId, @Nullable I input, @NotNull IOFunctions.IOFunction<O, R> outputConverter) {
        this.serverState = de.unijena.bioinf.ms.rest.model.JobState.INITIAL;
        this.outputConverter = outputConverter;
        this.input = input;
        if (jobId != null)
            submissionAck(getJobId());
    }


    public ID getJobId() {
        return jobId;
    }


    public void reset() {
        setServerState(de.unijena.bioinf.ms.rest.model.JobState.SUBMITTED);
        errorMessage = null;
        runningSince = null;
    }

    public WebJJob<I, O, R, ID> update(JobUpdate<?, ID> message) {
        updateTyped((JobUpdate<O, ID>) message);
        return this;
    }

    public I getInput() {
        return input;
    }

    public void setInput(I input) {
        this.input = input;
    }

    //post submission update
    protected synchronized void updateTyped(@NotNull JobUpdate<O, ID> update) {
        updateState(update);
        try {
            switch (serverState) {
                case DONE:
                    finish(outputConverter.apply(update.getData()));
                    break;
                case CRASHED:
                    crash(new Exception(errorMessage));
                    break;
                case CANCELED:
                    cancel();
                    break;
                default:
                    checkForTimeout();
            }
        } catch (IOException e) {
            crash(e);
        }
    }

    //pre submission update
    public synchronized void submissionAck(@Nullable ID jobId) {
        if (jobId == null && this.jobId == null)
            crash(new IllegalStateException("WebJJob was confirmed to be submitted to server but has not received an ID!"));

        if (jobId != null)
            this.jobId = jobId;

        this.submissionTime = System.currentTimeMillis();
        setServerState(de.unijena.bioinf.ms.rest.model.JobState.SUBMITTED);
    }

    protected void checkForTimeout() {
        if (System.currentTimeMillis() - submissionTime > WEB_API_JOB_TIME_OUT) {
            errorMessage = "Prediction canceled by client timeout. A timeout of \"" + WEB_API_JOB_TIME_OUT + "ms\" was reached.";
            crash(new TimeoutException(errorMessage));
        }
    }

    protected void checkIdOrThrow(@NotNull final JobUpdate<O, ID> update) {
        if (!getJobId().equals(update.getID()))
            throw new IllegalArgumentException("Update jobsId differs from jobId: " + jobId + " vs. " + update.getID());
    }

    public boolean updateState(@NotNull final JobUpdate<O, ID> update) {
        checkIdOrThrow(update);
        if (runningSince == null && update.getState() == de.unijena.bioinf.ms.rest.model.JobState.FETCHED.ordinal())
            runningSince = System.currentTimeMillis();
        if (serverState != update.getStateEnum()) {
            setServerState(update.getStateEnum());
            errorMessage = update.getErrorMessage();
            return true;
        }
        return false;
    }


    protected synchronized void setServerState(de.unijena.bioinf.ms.rest.model.JobState state) {
        this.serverState = state;
    }

    public void setCountingHash(Integer countingHash) {
        this.countingHash = countingHash;
    }

    public Optional<Integer> getJobCountingHash() {
        return Optional.ofNullable(countingHash);
    }

    public long getSubmissionTime() {
        return submissionTime;
    }

    //true if timedout
    public boolean checkRunningTimeout() {
        if (runningSince == null)
            return false;
        return (System.currentTimeMillis() - runningSince) > WEB_API_RUNNING_JOB_TIME_OUT;
    }
}
