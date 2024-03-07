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
import de.unijena.bioinf.jjobs.JobStateEvent;
import de.unijena.bioinf.ms.frontend.BackgroundRuns;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.middleware.model.compute.*;
import de.unijena.bioinf.ms.middleware.model.events.BackgroundComputationsStateEvent;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvents;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.ms.middleware.service.projects.SiriusProjectSpaceImpl;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class SiriusProjectSpaceComputeService extends AbstractComputeService<SiriusProjectSpaceImpl> {

    private final ConcurrentHashMap<String, BackgroundRuns<?, ?>> backgroundRuns = new ConcurrentHashMap<>();

    public SiriusProjectSpaceComputeService(EventService<?> eventService) {
        super(eventService);

    }

    private BackgroundRuns<?, ?> backgroundRuns(SiriusProjectSpaceImpl psm) {
        return backgroundRuns.computeIfAbsent(psm.getProjectId(), p -> {
            BackgroundRuns br = new BackgroundRuns<>(psm.getProjectSpaceManager());
            br.addUnfinishedRunsListener(evt -> {
                if (evt instanceof BackgroundRuns<?, ?>.ChangeEvent) {
                    BackgroundRuns.ChangeEvent e = (BackgroundRuns<?, ?>.ChangeEvent) evt;
                    eventService.sendEvent(ServerEvents.newComputeStateEvent(
                            BackgroundComputationsStateEvent.builder()
                                    .numberOfJobs(e.getNumOfRunsNew())
                                    .numberOfRunningJobs(e.getNumOfUnfinishedNew())
                                    .numberOfFinishedJobs(e.getNumOfRunsNew() - e.getNumOfUnfinishedNew())
                                    .affectedJobs(e.getEffectedJobs().stream().map(j -> extractJobId((BackgroundRuns<?, ?>.BackgroundRunJob) j, EnumSet.of(Job.OptField.progress, Job.OptField.affectedIds))).toList())
                                    .build()
                            , psm.getProjectId()));
                }
            });
            return br;
        });
    }

    private void removeBackgroundRuns(SiriusProjectSpaceImpl psm) {
        BackgroundRuns<?, ?> br = backgroundRuns.remove(psm.getProjectId());
        if (br != null)
            br.cancelAllRuns();
    }

    private void registerServerEventListener(BackgroundRuns<?, ?>.BackgroundRunJob run, String projectId) {
        run.addJobProgressListener(evt ->
                eventService.sendEvent(ServerEvents.newJobEvent(
                        extractJobId(run, EnumSet.of(Job.OptField.progress)), projectId)));
    }

    @Nullable
    private List<CompoundContainerId> extractCompoundIds(@NotNull AbstractSubmission jobSubmission,
                                                         @NotNull ProjectSpaceManager<?> psm) {
        List<CompoundContainerId> compounds = null;

        if (jobSubmission.getCompoundIds() == null || jobSubmission.getCompoundIds().isEmpty()) {
            if (jobSubmission.getAlignedFeatureIds() != null && !jobSubmission.getAlignedFeatureIds().isEmpty()) {
                compounds = new ArrayList<>();
                for (String cid : jobSubmission.getAlignedFeatureIds()) {
                    compounds.add(psm.projectSpace().findCompound(cid)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT,
                                    "Compound with id '" + cid + "' does not exist!'. No job has been started!")));
                }
            }
        } else {
            compounds = new ArrayList<>();
            Set<String> cids = new HashSet<>(jobSubmission.getCompoundIds());
            Set<String> fids = jobSubmission.getAlignedFeatureIds() != null
                    ? new HashSet<>(jobSubmission.getAlignedFeatureIds())
                    : Set.of();

            psm.projectSpace()
                    .filteredIterator(id -> id.getGroupId().map(cids::contains).orElse(false)
                            || fids.contains(id.getDirectoryName()))
                    .forEachRemaining(compounds::add);
        }

        return compounds;
    }

    @Override
    public Job createAndSubmitJob(@NotNull SiriusProjectSpaceImpl psmI, JobSubmission jobSubmission,
                                  @NotNull EnumSet<Job.OptField> optFields) {
        BackgroundRuns<?, ?> br = backgroundRuns(psmI);
        List<CompoundContainerId> compounds = extractCompoundIds(jobSubmission, br.getProjectSpaceManager());

        try {
            List<String> commandList = jobSubmission.asCommand();
            BackgroundRuns<?, ?>.BackgroundRunJob run = backgroundRuns(psmI).runCommand(commandList, compounds);
            registerServerEventListener(run, psmI.getProjectId());
            return extractJobId(run, optFields);
        } catch (Exception e) {
            log.error("Cannot create Job Command!", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create Job Command!", e);
        }
    }

    @Override
    public Job createAndSubmitJob(@NotNull SiriusProjectSpaceImpl psmI, List<String> commandList,
                                  @Nullable Iterable<String> alignedFeatureIds,
                                  @Nullable InputFilesOptions toImport,
                                  @NotNull EnumSet<Job.OptField> optFields) {
        try {

            //create instance iterator from ids
            BackgroundRuns<?, ?>.BackgroundRunJob run;
            if (alignedFeatureIds != null) {
                final Set<String> ids = new HashSet<>();
                alignedFeatureIds.forEach(ids::add);
                run = backgroundRuns(psmI).runCommand(commandList, cid -> ids.contains(cid.getDirectoryName()), null, toImport);
            } else {
                run = backgroundRuns(psmI).runCommand(commandList, (Iterable) null, toImport);
            }

            registerServerEventListener(run, psmI.getProjectId());
            return extractJobId(run, optFields);
        } catch (Exception e) {
            log.error("Cannot create Job Command!", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create Job Command!", e);
        }
    }

    @Override
    public Job createAndSubmitImportJob(@NotNull SiriusProjectSpaceImpl psmI, ImportLocalFilesSubmission jobSubmission,
                                        @NotNull EnumSet<Job.OptField> optFields) {
        InputFilesOptions inputFiles = new InputFilesOptions();
        inputFiles.msInput = new InputFilesOptions.MsInput();
        inputFiles.msInput.setAllowMS1Only(jobSubmission.isAllowMs1OnlyData());
        inputFiles.msInput.setIgnoreFormula(jobSubmission.isIgnoreFormulas());
        inputFiles.msInput.setInputPath(jobSubmission.getInputPaths().stream().map(Path::of)
                .collect(Collectors.toList()));

        boolean alignLCMSRuns = jobSubmission.isAlignLCMSRuns() && inputFiles.msInput.msParserfiles.keySet().stream()
                .anyMatch(p -> p.getFileName().toString().toLowerCase().endsWith("mzml")
                        || p.getFileName().toString().toLowerCase().endsWith("mzxml"));
        System.out.println("Alignment: " + alignLCMSRuns);

        return createAndSubmitJob(psmI, alignLCMSRuns
                        ? List.of("lcms-align")
                        : List.of("project-space", "--keep-open"),
                null, inputFiles, optFields);
    }

    @Override
    public Job createAndSubmitPeakListImportJob(@NotNull SiriusProjectSpaceImpl psmI, AbstractImportSubmission importSubmission,
                                                @NotNull EnumSet<Job.OptField> optFields) {
        BackgroundRuns<?, ?>.BackgroundRunJob run = backgroundRuns(psmI)
                .runImport(importSubmission.asInputResource(), importSubmission.isIgnoreFormulas(), importSubmission.isAllowMs1OnlyData());
        registerServerEventListener(run, psmI.getProjectId());
        return extractJobId(run, optFields);
    }

    @Override
    public Job createAndSubmitCommandJob(@NotNull SiriusProjectSpaceImpl psmI, CommandSubmission commandSubmission,
                                         @NotNull EnumSet<Job.OptField> optFields) {
        BackgroundRuns<?, ?> br = backgroundRuns(psmI);
        List<CompoundContainerId> compounds = extractCompoundIds(commandSubmission, br.getProjectSpaceManager());
        InputFilesOptions inputFiles = null;
        if (commandSubmission.getInputPaths() != null && !commandSubmission.getInputPaths().isEmpty()){
            inputFiles = new InputFilesOptions();
            inputFiles.msInput = new InputFilesOptions.MsInput();
        }

        try {
            BackgroundRuns<?, ?>.BackgroundRunJob run = br.runCommand(commandSubmission.getCommand(), compounds, inputFiles);
            registerServerEventListener(run, psmI.getProjectId());
            return extractJobId(run, optFields);
        } catch (Exception e) {
            log.error("Cannot create Job Command!", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create Job Command!", e);
        }
    }

    @Override
    public Job deleteJob(@NotNull SiriusProjectSpaceImpl psm, String jobId, boolean cancelIfRunning, boolean awaitDeletion, @NotNull EnumSet<Job.OptField> optFields) {

        BackgroundRuns<?, ?>.BackgroundRunJob j = getJob(psm, jobId);
        if (j.isFinished()) {
            backgroundRuns(psm).removeFinishedRun(j.getRunId());
        } else {
            if (cancelIfRunning)
                j.cancel();
            if (awaitDeletion) {
                j.getResult(); //use state-lock to ensure that state is update when deleting cancelled job.
//                j.withStateLockDo(() ->  BackgroundRuns.removeRun(j.getRunId()));
//                j.setState(cancelIfRunning ? JJob.JobState.CANCELED : JJob.JobState.DONE);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //ignore
                }
                backgroundRuns(psm).removeFinishedRun(j.getRunId());
            } else {
                j.addPropertyChangeListener(JobStateEvent.JOB_STATE_EVENT, evt -> {
                    if (evt instanceof JobStateEvent) {
                        final BackgroundRuns<?, ?>.BackgroundRunJob jj = (BackgroundRuns<?, ?>.BackgroundRunJob) evt.getSource();
                        if (jj.isFinished())
                            backgroundRuns(psm).removeFinishedRun(jj.getRunId());
                    }
                });
                //may already have been finished during listener registration
                if (j.isFinished())
                    backgroundRuns(psm).removeFinishedRun(j.getRunId());
            }
        }
        return extractJobId(j, optFields);
    }

    @Override
    public List<Job> deleteJobs(@NotNull SiriusProjectSpaceImpl psm, boolean cancelIfRunning, boolean awaitDeletion, boolean closeProject, @NotNull EnumSet<Job.OptField> optFields) {
        List<Job> jobs = getJobs(psm, Pageable.unpaged(), EnumSet.noneOf(Job.OptField.class))
                .stream().map(j -> deleteJob(psm, j.getId(), cancelIfRunning, awaitDeletion, optFields)).toList();
        if (closeProject)
            removeBackgroundRuns(psm);

        return jobs;
    }

    public BackgroundRuns<?, ?>.BackgroundRunJob getJob(@NotNull SiriusProjectSpaceImpl psm, String jobId) {
        try {
            int intId = Integer.parseInt(jobId);
            BackgroundRuns<?, ?>.BackgroundRunJob j = backgroundRuns(psm).getRunById(intId);
            if (j == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job with ID '" + jobId + " does not Exist! Hint: It is either already finished and has been auto removed (if enabled) or the ID never existed.");

            if (!psm.getProjectSpaceManager().equals(j.getProject()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job with ID '" + jobId + " is not part of the requested project but does exist! Hint: Request the job with the correct projectId.");

            return j;
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Illegal JobId '" + jobId + "! Hint: JobIds are usually integer numbers.", e);
        }
    }

    @Override
    public Job getJob(@NotNull SiriusProjectSpaceImpl psm, String jobId, @NotNull EnumSet<Job.OptField> optFields) {
        return extractJobId(getJob(psm, jobId), optFields);
    }

    @Override
    public Page<Job> getJobs(@NotNull SiriusProjectSpaceImpl psm, @NotNull Pageable pageable, @NotNull EnumSet<Job.OptField> optFields) {
        if (pageable.isUnpaged())
            return new PageImpl<>(backgroundRuns(psm).getRunsStr()
                    .map(j -> extractJobId(j, optFields)).toList());

        long size = backgroundRuns(psm).getRunsStr().count();
        return new PageImpl<>(backgroundRuns(psm).getRunsStr()
                .skip(pageable.getOffset()).limit(pageable.getPageSize())
                .map(j -> extractJobId(j, optFields))
                .toList(), pageable, size);
    }

    @Override
    public boolean hasJobs(@NotNull SiriusProjectSpaceImpl psm, boolean includeFinished) {
        if (includeFinished)
            return !backgroundRuns(psm).getRunningRuns().isEmpty();
        return backgroundRuns(psm).getRunsStr().findAny().isPresent();
    }

    @Override
    public JJob<?> getJJob(@NotNull SiriusProjectSpaceImpl psm, String jobId) {
        return backgroundRuns(psm).getRunById(Integer.parseInt(jobId));
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
