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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JsonDeserialize(using = JobUpdateDeserializer.class)
public class JobUpdate<D> extends JobBase  implements de.unijena.bioinf.ms.webapi.JobUpdate<D, JobId> {
    @Nullable
    public final D data;

    public JobUpdate(JobBase base, @Nullable D data) {
        super(base.jobId, base.state, base.jobTable, base.errorMessage);
        this.data = data;
    }

    public JobUpdate(Long jobId, JobState state, @NotNull JobTable table, String errorMessage, @Nullable D data) {
        super(jobId, state, table, errorMessage);
        this.data = data;
    }

    public JobUpdate(Long id, JobState state, @NotNull JobTable table, @Nullable D data) {
        this(id, state,  table, null, data);
    }

    public JobUpdate(JobId jobId, JobState state, @Nullable D data) {
        this(jobId.jobId, state, jobId.jobTable, data);
    }

    @Override
    public Class<? extends D> getDataType() {
        return (Class<? extends D>) getJobTable().jobOutputType;
    }

    @Override
    public @Nullable D getData() {
        return data;
    }

    public JobId getGlobalId() {
        return new JobId(jobId, jobTable);
    }
}
