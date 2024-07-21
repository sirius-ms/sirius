/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.middleware.model.compute;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Identifier created by the SIRIUS Nightsky API for a newly created Job.
 * Object can be enriched with Job status/progress information ({@link JobProgress}) and/or Job command information.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Job {
    @Schema(enumAsRef = true, name = "JobOptField", nullable = true)
    public enum OptField {none, command, progress, affectedIds}

    /**
     * Unique identifier to access the job via the API
     */
    String id;
    /**
     * Command string of the executed Task
     */
    @Nullable
    String command;

    /**
     * Optional progress information of this job
     */
    @Nullable
    JobProgress progress;

    /**
     * List of compoundIds that are affected by this job.
     * This lis will also contain compoundIds where not all features of the compound are affected by the job.
     * If this job is creating compounds (e.g. data import jobs) this value will be NULL until the jobs has finished
     */
    @Nullable
    List<String> affectedCompoundIds;

    /**
     * List of alignedFeatureIds that are affected by this job.
     * If this job is creating features (e.g. data import jobs) this value will be NULL until the jobs has finished
     */
    @Nullable
    List<String> affectedAlignedFeatureIds;
}
