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

package de.unijena.bioinf.ms.rest.model.worker;

import de.unijena.bioinf.ms.rest.model.JobTable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.Collectors;

//specifies job from which jobtable a worker has to use see @JobTable
public enum WorkerType {
    FORMULA_ID(EnumSet.noneOf(JobTable.class)), //todo has to be implemented.
    FINGER_ID(EnumSet.of(JobTable.JOBS_FINGERID)),
    IOKR(EnumSet.noneOf(JobTable.class)), //todo has to be implemented.
    CANOPUS(EnumSet.of(JobTable.JOBS_CANOPUS)),
    COVTREE(EnumSet.of(JobTable.JOBS_COVTREE));

    private final EnumSet<JobTable> jobTables;

    WorkerType(EnumSet<JobTable> jobTableNames) {
        jobTables = jobTableNames;
    }

    public EnumSet<JobTable> jobTables() {
        return jobTables;
    }

    //todo can we do this generic for all type enums
    public static EnumSet<WorkerType> parse(@NotNull String workerTypes, String regexDelimiter) {
        return Arrays.stream(workerTypes.split(regexDelimiter))
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter((s) -> !s.isEmpty())
                .map(WorkerType::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(WorkerType.class)));
    }

    public static EnumSet<WorkerType> parse(@NotNull String workerTypes) {
        return parse(workerTypes, ",");
    }
}
