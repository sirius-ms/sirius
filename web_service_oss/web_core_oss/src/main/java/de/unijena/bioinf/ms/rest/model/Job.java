
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;

@Setter
@Getter
public abstract class Job<O> extends JobBase {
    protected String workerPrefix;
    protected Timestamp submissionTime;
    protected Timestamp lockedByWorker;
    protected int numOfLocks;
    protected String cid;
    protected String userID;
    protected String version;

    protected Job(String workerPrefix, JobState state, JobTable table) {
        this(workerPrefix, null, state, table);
    }

    protected Job(String workerPrefix, Long jobId, JobState state,  JobTable table) {
        super(jobId, state, table);
        this.workerPrefix = workerPrefix;
        this.version = FingerIDProperties.fingeridMinorVersion();
    }

    @Nullable
    @JsonIgnore
    public abstract O extractOutput();

    @JsonIgnore
    public JobUpdate<O> asUpdate() {
        return new JobUpdate<>(this, extractOutput());
    }
}
