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

public class WebJJob<I, O, R, ID> extends WaiterJJob<R> implements InputJJob<I,R> {
    public static long WEB_API_JOB_TIME_OUT = PropertyManager.getLong("de.unijena.bioinf.fingerid.web.job.timeout", 1000L * 60L * 60L); //default 1h

    protected I input;
    protected final ID jobId;
    protected final IOFunctions.IOFunction<O, R> outputConverter;

    protected volatile de.unijena.bioinf.ms.rest.model.JobState serverState;
    protected volatile long submissionTime;
    protected volatile String errorMessage;

    @Nullable
    public Integer countingHash;

    public WebJJob(@NotNull ID jobId, @NotNull IOFunctions.IOFunction<O, R> outputConverter) {
        this(jobId, null, outputConverter);
    }

    public WebJJob(@NotNull ID jobId, @Nullable I input, @NotNull IOFunctions.IOFunction<O, R> outputConverter) {
        this.serverState = de.unijena.bioinf.ms.rest.model.JobState.INITIAL;
        this.outputConverter = outputConverter;
        this.jobId = jobId;
        this.input = input;
    }


    public ID getJobId() {
        return jobId;
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

    protected synchronized void updateTyped(@NotNull JobUpdate<O, ID> update) {
        updateState(update);
        checkForTimeout();

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
            }
        } catch (IOException e) {
            crash(e);
        }
    }

    public void basicAck() {
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

    public Optional<Integer> getJobCountingHash(){
        return Optional.ofNullable(countingHash);
    }
}
