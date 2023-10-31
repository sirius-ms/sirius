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

package de.unijena.bioinf.ms.middleware.service.compute;

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.middleware.model.compute.ImportLocalFilesSubmission;
import de.unijena.bioinf.ms.middleware.model.compute.ImportStringSubmission;
import de.unijena.bioinf.ms.middleware.model.compute.Job;
import de.unijena.bioinf.ms.middleware.model.compute.JobSubmission;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.EnumSet;
import java.util.List;

public interface ComputeService<P extends Project> extends DisposableBean {

    Job createAndSubmitJob(P psm, JobSubmission jobSubmission, @NotNull EnumSet<Job.OptField> optFields);

    Job createAndSubmitJob(P psm, List<String> commandList, @Nullable Iterable<String> alignedFeatureIds, @Nullable InputFilesOptions toImport, @NotNull EnumSet<Job.OptField> optFields);

    Job createAndSubmitImportJob(P psm, ImportLocalFilesSubmission jobSubmission, @NotNull EnumSet<Job.OptField> optFields);

    Job createAndSubmitImportJob(P psm, ImportStringSubmission jobSubmission, @NotNull EnumSet<Job.OptField> optFields);


    default Job deleteJob(String jobId, boolean cancelIfRunning, boolean awaitDeletion, @NotNull EnumSet<Job.OptField> optFields) {
        return deleteJob(null, jobId, cancelIfRunning, awaitDeletion, optFields);
    }

    Job deleteJob(@Nullable P psm, String jobId, boolean cancelIfRunning, boolean awaitDeletion, @NotNull EnumSet<Job.OptField> optFields);

    Job getJob(@Nullable P psm, String jobId, @NotNull EnumSet<Job.OptField> optFields);

    default Page<Job> getJobs(@NotNull Pageable pageable, @NotNull EnumSet<Job.OptField> optFields) {
        return getJobs(null, pageable, optFields);
    }

    Page<Job> getJobs(@Nullable P psm, @NotNull Pageable pageable, @NotNull EnumSet<Job.OptField> optFields);

    JJob<?> getJJob(String jobId);
}
