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

import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.ms.frontend.BackgroundRuns;
import de.unijena.bioinf.ms.middleware.model.compute.Job;
import de.unijena.bioinf.ms.middleware.model.compute.JobProgress;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractComputeService<P extends Project> implements ComputeService<P> {


    protected final EventService<?> eventService;

    public AbstractComputeService(EventService<?> eventService) {
        this.eventService = eventService;
    }

    protected Job extractJobId(BackgroundRuns<?, ?>.BackgroundRunJob runJob, @NotNull EnumSet<Job.OptField> optFields) {
        Job id = new Job();
        id.setId(String.valueOf(runJob.getRunId()));
        if (optFields.contains(Job.OptField.command))
            id.setCommand(runJob.getCommand());
        if (optFields.contains(Job.OptField.progress))
            id.setProgress(extractProgress(runJob));
        if (optFields.contains(Job.OptField.affectedIds)){
            id.setAffectedAlignedFeatureIds(extractEffectedAlignedFeatures(runJob));
            id.setAffectedCompoundIds(extractCompoundIds(runJob));
        }

        return id;
    }

    protected List<String> extractEffectedAlignedFeatures(BackgroundRuns<?, ?>.BackgroundRunJob runJob) {
        if (runJob.getInstanceIds() == null || runJob.getInstanceIds().isEmpty())
            return List.of();
        return runJob.getInstanceIds().stream().map(CompoundContainerId::getDirectoryName).collect(Collectors.toList());
    }

    protected List<String> extractCompoundIds(BackgroundRuns<?, ?>.BackgroundRunJob runJob) {
        if (runJob.getInstanceIds() == null || runJob.getInstanceIds().isEmpty())
            return List.of();
        return runJob.getInstanceIds().stream()
                .map(CompoundContainerId::getGroupId).filter(Optional::isPresent).flatMap(Optional::stream)
                .distinct().collect(Collectors.toList());
    }

    protected JobProgress extractProgress(BackgroundRuns<?, ?>.BackgroundRunJob runJob) {
        JobProgress p = new JobProgress();
        p.setState(runJob.getState());

        JobProgressEvent evt = runJob.currentProgress();
        if (evt == null)
            evt = new JobProgressEvent(runJob);
        p.setIndeterminate(!evt.isDetermined());
        p.setCurrentProgress(evt.getProgress());
        p.setMaxProgress(evt.getMaxValue());
        p.setMessage(evt.getMessage());
        if (runJob.isUnSuccessfulFinished()) {
            try { //collect error message from exception
                runJob.awaitResult();
            } catch (ExecutionException e) {
                if (e.getCause() != null)
                    p.setErrorMessage(e.getCause().getMessage());
                else
                    p.setErrorMessage(e.getMessage());
            }
        }
        return p;
    }
}
