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

package de.unijena.bioinf.ms.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobBase {
    @NotNull
    protected final JobTable jobTable;

    protected Long jobId;
    protected JobState state;
    protected String errorMessage;


    //just for jackson
    private JobBase() {
        this(null, null, null);
    }

    protected JobBase(Long jobId, JobState state, @NotNull JobTable table) {
        this(jobId, state, table, null);
    }

    protected JobBase(Long jobId, JobState state, @NotNull JobTable table, String errorMessage) {
        this.jobId = jobId;
        this.state = state;
        this.jobTable = table;
        this.errorMessage = errorMessage;
    }

    public JobState getStateEnum() {
        return state;
    }

    public void setStateEnum(JobState state) {
        this.state = state;
    }

    public Integer getState() {
        return Optional.ofNullable(getStateEnum()).map(JobState::id).orElse(null);
    }

    public void setState(Integer state) {
        setStateEnum(state != null ? JobState.withId(state) : null);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public JobTable getJobTable() {
        return jobTable;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }
}
