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
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobStateEvent;
import de.unijena.bioinf.ms.backgroundruns.BackgroundRuns;
import de.unijena.bioinf.ms.frontend.workflow.InstanceBufferFactory;
import de.unijena.bioinf.ms.middleware.model.compute.*;
import de.unijena.bioinf.ms.middleware.model.events.BackgroundComputationsStateEvent;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvents;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.projectspace.Instance;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class ComputeServiceImpl implements ComputeService {

    private final ConcurrentHashMap<String, BackgroundRuns> backgroundRuns = new ConcurrentHashMap<>();
    private final InstanceBufferFactory<?> instanceBufferFactory;
    protected final EventService<?> eventService;


    public ComputeServiceImpl(EventService<?> eventService, InstanceBufferFactory<?> instanceBufferFactory) {
        this.eventService = eventService;
        this.instanceBufferFactory = instanceBufferFactory;
    }

    @Override
    public boolean isInstanceComputing(@NotNull Project<?> psm, String alignedFeatureId) {
        return backgroundRuns(psm).isInstanceComputing(alignedFeatureId);
    }

    private BackgroundRuns backgroundRuns(Project<?> project) {
        return backgroundRuns.computeIfAbsent(project.getProjectId(), p -> {
            BackgroundRuns br = new BackgroundRuns(project.getProjectSpaceManager(), instanceBufferFactory);
            br.addUnfinishedRunsListener(evt -> {
                if (evt instanceof BackgroundRuns.ChangeEvent e) {
                    eventService.sendEvent(ServerEvents.newComputeStateEvent(
                            BackgroundComputationsStateEvent.builder()
                                    .numberOfJobs(e.getNumOfRunsNew())
                                    .numberOfRunningJobs(e.getNumOfUnfinishedNew())
                                    .numberOfFinishedJobs(e.getNumOfRunsNew() - e.getNumOfUnfinishedNew())
                                    .affectedJobs(e.getEffectedJobs().stream().map(j -> extractJobId(j, EnumSet.of(Job.OptField.progress, Job.OptField.affectedIds))).toList())
                                    .build()
                            , project.getProjectId()));
                }
            });
            return br;
        });
    }

    private void removeBackgroundRuns(Project<?> project) {
        BackgroundRuns br = backgroundRuns.remove(project.getProjectId());
        if (br != null)
            br.cancelAllRuns();
    }

    private void registerServerEventListener(BackgroundRuns.BackgroundRunJob run, String projectId) {
        run.addJobProgressListener(evt ->
                eventService.sendEvent(ServerEvents.newJobEvent(
                        extractJobId(run, EnumSet.of(Job.OptField.progress)), projectId)));
    }

    @Nullable
    private List<Instance> extractCompoundIds(@NotNull AbstractSubmission jobSubmission,
                                              @NotNull Project<?> project) {
        List<Instance> compounds = null;

        if (jobSubmission.getCompoundIds() == null || jobSubmission.getCompoundIds().isEmpty()) {
            if (jobSubmission.getAlignedFeatureIds() != null && !jobSubmission.getAlignedFeatureIds().isEmpty()) {
                compounds = new ArrayList<>();
                for (String cid : jobSubmission.getAlignedFeatureIds())
                    compounds.add(loadInstance(project, cid));
            }
        } else {
            compounds = new ArrayList<>();
            Set<String> cids = new HashSet<>(jobSubmission.getCompoundIds());
            Set<String> fids = jobSubmission.getAlignedFeatureIds() != null
                    ? new HashSet<>(jobSubmission.getAlignedFeatureIds())
                    : Set.of();

            StreamSupport.stream(project.getProjectSpaceManager().spliterator(), false).filter(
                    instance -> instance.getCompoundId().map(cids::contains).orElse(false) || fids.contains(instance.getId())
            ).forEach(compounds::add);
        }

        return compounds;
    }

    protected Job extractJobId(BackgroundRuns.BackgroundRunJob runJob, @NotNull EnumSet<Job.OptField> optFields) {
        Job id = new Job();
        id.setId(String.valueOf(runJob.getRunId()));
        if (optFields.contains(Job.OptField.command))
            id.setCommand(runJob.getCommand());
        if (optFields.contains(Job.OptField.progress))
            id.setProgress(extractProgress(runJob));
        if (optFields.contains(Job.OptField.affectedIds)) {
            id.setAffectedAlignedFeatureIds(extractEffectedAlignedFeatures(runJob));
            id.setAffectedCompoundIds(extractCompoundIds(runJob));
        }

        return id;
    }

    private static Instance loadInstance(Project<?> project, String alignedFeatureId){
        return project.getProjectSpaceManager().findInstance(alignedFeatureId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance with ID " + alignedFeatureId + " not found in project + " + project.getProjectId() + ".")
        );
    }

    private List<String> extractEffectedAlignedFeatures(BackgroundRuns.BackgroundRunJob runJob) {
        return runJob.getInstancesStr().map(Instance::getId).collect(Collectors.toList());
    }

    private List<String> extractCompoundIds(BackgroundRuns.BackgroundRunJob runJob) {
        return runJob.getInstancesStr()
                .map(Instance::getCompoundId)
                .filter(Optional::isPresent)
                .flatMap(Optional::stream)
                .distinct().collect(Collectors.toList());
    }

    private JobProgress extractProgress(BackgroundRuns.BackgroundRunJob runJob) {
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
            Exception ex = runJob.getException();
            if (ex != null)
                p.setErrorMessage(ex.getMessage());
        }
        return p;
    }

    @Override
    public Job createAndSubmitJob(@NotNull Project<?> psmI, JobSubmission jobSubmission,
                                  @NotNull EnumSet<Job.OptField> optFields) {
        Iterable<Instance> instances = extractCompoundIds(jobSubmission, psmI);
        if (instances == null)
            instances = psmI.getProjectSpaceManager();

        try {
            List<String> commandList = jobSubmission.asCommand();
            BackgroundRuns.BackgroundRunJob run = backgroundRuns(psmI).runCommand(commandList, instances);
            registerServerEventListener(run, psmI.getProjectId());
            return extractJobId(run, optFields);
        } catch (Exception e) {
            log.error("Cannot create Job Command!", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create Job Command!", e);
        }
    }

    @Override
    public Job createAndSubmitJob(@NotNull Project<?> project, List<String> commandList,
                                  @Nullable Iterable<String> alignedFeatureIds,
                                  @NotNull EnumSet<Job.OptField> optFields) {
        try {
            //create instance iterator from ids
            BackgroundRuns.BackgroundRunJob run;
            if (alignedFeatureIds != null) {
                List<Instance> instances = new ArrayList<>();
                alignedFeatureIds.forEach(id -> instances.add(loadInstance(project, id)));
                run = backgroundRuns(project).runCommand(commandList, instances);
            } else {
                run = backgroundRuns(project).runCommand(commandList, project.getProjectSpaceManager());
            }

            registerServerEventListener(run, project.getProjectId());
            return extractJobId(run, optFields);
        } catch (Exception e) {
            log.error("Cannot create Job Command!", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create Job Command!", e);
        }
    }

    @Override
    public Job createAndSubmitMsDataImportJob(@NotNull Project<?> project, AbstractImportSubmission importSubmission,
                                              @NotNull EnumSet<Job.OptField> optFields) {
        BackgroundRuns.BackgroundRunJob run = backgroundRuns(project).runImportMsData(importSubmission);
        registerServerEventListener(run, project.getProjectId());
        return extractJobId(run, optFields);
    }

    @Override
    public Job createAndSubmitPeakListImportJob(@NotNull Project<?> project, AbstractImportSubmission importSubmission,
                                                @NotNull EnumSet<Job.OptField> optFields) {
        BackgroundRuns.BackgroundRunJob run = backgroundRuns(project)
                .runImportPeakData(importSubmission.asInputResource(), importSubmission.isIgnoreFormulas(), importSubmission.isAllowMs1OnlyData());
        registerServerEventListener(run, project.getProjectId());
        return extractJobId(run, optFields);
    }

    @Override
    public Job createAndSubmitCommandJob(@NotNull Project<?> project, CommandSubmission commandSubmission,
                                         @NotNull EnumSet<Job.OptField> optFields) {
        BackgroundRuns br = backgroundRuns(project);
        Iterable<Instance> instances = extractCompoundIds(commandSubmission, project);
        if (instances == null)
            instances = project.getProjectSpaceManager();
        try {
            BackgroundRuns.BackgroundRunJob run = br.runCommand(commandSubmission.getCommand(), instances);
            registerServerEventListener(run, project.getProjectId());
            return extractJobId(run, optFields);
        } catch (Exception e) {
            log.error("Cannot create Job Command!", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create Job Command!", e);
        }
    }

    @Override
    public Job deleteJob(@NotNull Project<?> project, String jobId, boolean cancelIfRunning, boolean awaitDeletion, @NotNull EnumSet<Job.OptField> optFields) {
        BackgroundRuns.BackgroundRunJob j = findJob(project, jobId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NO_CONTENT, "Job with ID '" + jobId + " not found! Hint: It is either already finished and has been auto removed (if enabled) or the ID never existed. Doing nothing!"));

        if (j.isFinished()) {
            backgroundRuns(project).removeFinishedRun(j.getRunId());
        } else {
            if (cancelIfRunning)
                j.cancel(false);

            if (awaitDeletion) {
                j.getResult(); //use state-lock to ensure that state is update when deleting cancelled job.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //ignore
                }
                backgroundRuns(project).removeFinishedRun(j.getRunId());
            } else {
                j.addPropertyChangeListener(JobStateEvent.JOB_STATE_EVENT, evt -> {
                    if (evt instanceof JobStateEvent) {
                        final BackgroundRuns.BackgroundRunJob jj = (BackgroundRuns.BackgroundRunJob) evt.getSource();
                        if (jj.isFinished())
                            backgroundRuns(project).removeFinishedRun(jj.getRunId());
                    }
                });
                //may already have been finished during listener registration
                if (j.isFinished())
                    backgroundRuns(project).removeFinishedRun(j.getRunId());
            }
        }
        return extractJobId(j, optFields);
    }

    @Override
    public List<Job> deleteJobs(@NotNull Project<?> project, boolean cancelIfRunning, boolean awaitDeletion, boolean closeProject, @NotNull EnumSet<Job.OptField> optFields) {
        List<Job> jobs = getJobs(project, Pageable.unpaged(), EnumSet.noneOf(Job.OptField.class))
                .stream().map(j -> deleteJob(project, j.getId(), cancelIfRunning, awaitDeletion, optFields)).toList();
        if (closeProject)
            removeBackgroundRuns(project);

        return jobs;
    }

    public Optional<BackgroundRuns.BackgroundRunJob> findJob(@NotNull Project<?> project, String jobId) {
        int intId = Integer.parseInt(jobId);
        return Optional.ofNullable(backgroundRuns(project).getRunById(intId));
    }

    public BackgroundRuns.BackgroundRunJob getJob(@NotNull Project<?> project, String jobId) {
        try {
            return findJob(project, jobId).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job with ID '" + jobId + " does not Exist! Hint: It is either already finished and has been auto removed (if enabled) or the ID never existed."));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Illegal JobId '" + jobId + "! Hint: JobIds are usually integer numbers.", e);
        }
    }

    @Override
    public Job getJob(@NotNull Project<?> project, String jobId, @NotNull EnumSet<Job.OptField> optFields) {
        return extractJobId(getJob(project, jobId), optFields);
    }

    @Override
    public Page<Job> getJobs(@NotNull Project<?> project, @NotNull Pageable pageable, @NotNull EnumSet<Job.OptField> optFields) {
        if (pageable.isUnpaged())
            return new PageImpl<>(backgroundRuns(project).getRuns().stream()
                    .map(j -> extractJobId(j, optFields)).toList());

        long size = backgroundRuns(project).getRuns().size();
        return new PageImpl<>(backgroundRuns(project).getRuns().stream()
                .skip(pageable.getOffset()).limit(pageable.getPageSize())
                .map(j -> extractJobId(j, optFields))
                .toList(), pageable, size);
    }

    @Override
    public boolean hasJobs(@NotNull Project<?> project, boolean includeFinished) {
        if (includeFinished)
            return !backgroundRuns(project).getRunningRuns().isEmpty();
        return backgroundRuns(project).getRuns().stream().findAny().isPresent();
    }

    @Override
    public JJob<?> getJJob(@NotNull Project<?> project, String jobId) {
        return backgroundRuns(project).getRunById(Integer.parseInt(jobId));
    }


    @Override
    public synchronized void destroy() {
        System.out.println("Destroy Compute Service...");
        backgroundRuns.forEach((pid, br) -> {
            if (br.hasActiveComputations())
                log.info("Cancelling running Background Jobs...");
            br.cancelAllRuns();
        });
        System.out.println("Destroy Compute Service DONE");
    }
}
