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

package de.unijena.bioinf.ms.middleware.service.compute;

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.middleware.model.compute.*;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.EnumSet;
import java.util.List;

public interface ComputeService extends DisposableBean {

    Job createAndSubmitJob(@NotNull Project<?> psm, JobSubmission jobSubmission, @NotNull EnumSet<Job.OptField> optFields);

    Job createAndSubmitJob(@NotNull Project<?> psm, List<String> commandList, @Nullable Iterable<String> alignedFeatureIds,
                           @NotNull EnumSet<Job.OptField> optFields);

    Job createAndSubmitMsDataImportJob(@NotNull Project<?> psm, AbstractImportSubmission importSubmission,
                                       @NotNull EnumSet<Job.OptField> optFields);

    Job createAndSubmitPeakListImportJob(@NotNull Project<?> psm, AbstractImportSubmission importSubmission,
                                         @NotNull EnumSet<Job.OptField> optFields);

    Job createAndSubmitCommandJob(@NotNull Project<?> psm, CommandSubmission commandSubmission,
                                  @NotNull EnumSet<Job.OptField> optFields);

    Job deleteJob(@NotNull Project<?> psm, String jobId, boolean cancelIfRunning, boolean awaitDeletion,
                  @NotNull EnumSet<Job.OptField> optFields);

    List<Job> deleteJobs(@NotNull Project<?> psm, boolean cancelIfRunning, boolean awaitDeletion, boolean closeProject,
                         @NotNull EnumSet<Job.OptField> optFields);

    Job getJob(@NotNull Project<?> psm, String jobId, @NotNull EnumSet<Job.OptField> optFields);

    Page<Job> getJobs(@NotNull Project<?> psm, @NotNull Pageable pageable, @NotNull EnumSet<Job.OptField> optFields);

    boolean hasJobs(@NotNull Project<?> psm, boolean includeFinished);

    JJob<?> getJJob(@NotNull Project<?> psm, String jobId);

    boolean isInstanceComputing(@NotNull Project<?> psm, String alignedFeatureId);
}
