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

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumFileSource;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobStateEvent;
import de.unijena.bioinf.ms.frontend.BackgroundRuns;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.middleware.model.compute.ImportLocalFilesSubmission;
import de.unijena.bioinf.ms.middleware.model.compute.ImportStringSubmission;
import de.unijena.bioinf.ms.middleware.model.compute.JobId;
import de.unijena.bioinf.ms.middleware.model.compute.JobSubmission;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.service.projects.SiriusProjectSpaceImpl;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SiriusProjectSpaceComputeService extends AbstractComputeService<SiriusProjectSpaceImpl> {

    private <I extends Instance, P extends ProjectSpaceManager<I>> JobId createAndSubmitJob(P psm, JobSubmission jobSubmission, @NotNull EnumSet<JobId.OptFields> optFields) {
        List<CompoundContainerId> compounds = null;

        if (jobSubmission.getCompoundIds() != null && !jobSubmission.getCompoundIds().isEmpty()) {
            compounds = new ArrayList<>(jobSubmission.getCompoundIds().size());
            for (String cid : jobSubmission.getCompoundIds()) {
                compounds.add(psm.projectSpace().findCompound(cid)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT,
                                "Compound with id '" + cid + "' does not exist!'. No job has been started!")));
            }
        }

        try {
            List<String> commandList = makeCommand(jobSubmission);
            BackgroundRuns.BackgroundRunJob<P, I> run = BackgroundRuns.runCommand(commandList, compounds, psm);
            return extractJobId(run, optFields);
        } catch (Exception e) {
            log.error("Cannot create Job Command!", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create Job Command!", e);
        }
    }

    private <I extends Instance, P extends ProjectSpaceManager<I>> JobId createAndSubmitJob(
            P psm,
            List<String> commandList,
            @Nullable Iterable<String> featureAlignId,
            @Nullable InputFilesOptions toImport,
            @NotNull EnumSet<JobId.OptFields> optFields
    ) {
        try {

            //create instance iterator from ids
            Iterable<I> instances = null;
            if (featureAlignId != null) {
                final Set<String> ids = new HashSet<>();
                featureAlignId.forEach(ids::add);
                instances = () -> psm.filteredIterator(cid -> ids.contains(cid.getDirectoryName()), null);
            }

            BackgroundRuns.BackgroundRunJob<P, I> run = BackgroundRuns.runCommand(commandList, instances, toImport, psm);
            return extractJobId(run, optFields);
        } catch (Exception e) {
            log.error("Cannot create Job Command!", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create Job Command!", e);
        }
    }

    private <I extends Instance, P extends ProjectSpaceManager<I>> JobId createAndSubmitImportJob(
            P psm, ImportLocalFilesSubmission jobSubmission,
            @NotNull EnumSet<JobId.OptFields> optFields
    ) {
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

        return createAndSubmitJob(psm, alignLCMSRuns ? List.of("lcms-align") : List.of("project-space"),
                null, inputFiles, optFields);
    }

    private <I extends Instance, P extends ProjectSpaceManager<I>> JobId createAndSubmitImportJob(
            P psm, ImportStringSubmission jobSubmission,
            @NotNull EnumSet<JobId.OptFields> optFields) {

        return extractJobId(BackgroundRuns.runJob(null, psm, (instances, project) -> {
            List<AlignedFeature> ids = new ArrayList<>();
            final @Nullable String sourceName = jobSubmission.getSourceName();
            final String ext = jobSubmission.getFormat().getExtension();

            GenericParser<Ms2Experiment> parser = new MsExperimentParser()
                    .getParserByExt(ext);


            try (BufferedReader bodyStream = new BufferedReader(new StringReader(jobSubmission.getData()))) {
                try (CloseableIterator<Ms2Experiment> it = parser.parseIterator(bodyStream, null)) {
                    while (it.hasNext()) {
                        Ms2Experiment next = it.next();
                        if (sourceName != null)     //todo import handling needs to be improved ->  this naming hassle is ugly
                            next.setAnnotation(SpectrumFileSource.class,
                                    new SpectrumFileSource(
                                            new File("./" + (sourceName.endsWith(ext) ? sourceName : sourceName + "." + ext.toLowerCase())).toURI()));

                        @NotNull Instance inst = psm.newCompoundWithUniqueId(next);
                        ids.add(AlignedFeature.of(inst.getID())); //todo how to add add features to hob update
                    }
                }
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); //todo io error usually means bad input data at this stage
            }
        }), optFields);
    }


    private JobId deleteJob(@Nullable ProjectSpaceManager<?> psm, String jobId, boolean cancelIfRunning, boolean awaitDeletion, @NotNull EnumSet<JobId.OptFields> optFields) {
        BackgroundRuns.BackgroundRunJob<?, ?> j = getJob(psm, jobId);
        if (j.isFinished()) {
            BackgroundRuns.removeRun(j.getRunId());
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
                BackgroundRuns.removeRun(j.getRunId());
            } else {
                j.addPropertyChangeListener(JobStateEvent.JOB_STATE_EVENT, evt -> {
                    if (evt instanceof JobStateEvent) {
                        final BackgroundRuns.BackgroundRunJob jj = (BackgroundRuns.BackgroundRunJob) evt.getSource();
                        if (jj.isFinished())
                            BackgroundRuns.removeRun(jj.getRunId());
                    }
                });
                //may already have been finished during listener registration
                if (j.isFinished())
                    BackgroundRuns.removeRun(j.getRunId());
            }
        }
        return extractJobId(j, optFields);
    }

    public BackgroundRuns.BackgroundRunJob<?, ?> getJob(@Nullable ProjectSpaceManager<?> psm, String jobId) {
        try {
            int intId = Integer.parseInt(jobId);
            BackgroundRuns.BackgroundRunJob<?, ?> j = BackgroundRuns.getActiveRunIdMap().get(intId);
            if (j == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job with ID '" + jobId + " does not Exist! Hint: It is either already finished an has bess auto removed (if enabled) or the ID never existed.");

            if (psm != null && !psm.equals(j.getProject()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job with ID '" + jobId + " is not part of the requested project but does exist! Hint: Request the job with the correct projectId.");

            return j;
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Illegal JobId '" + jobId + "! Hint: JobIds are usually integer numbers.", e);
        }
    }

    @Override
    public JobId createAndSubmitJob(SiriusProjectSpaceImpl psm, JobSubmission jobSubmission,
                                    @NotNull EnumSet<JobId.OptFields> optFields) {
        return createAndSubmitJob(psm.getProjectSpaceManager(), jobSubmission, optFields);
    }

    @Override
    public JobId createAndSubmitJob(SiriusProjectSpaceImpl psm, List<String> commandList,
                                    @Nullable Iterable<String> alignFeatureIds,
                                    @Nullable InputFilesOptions toImport,
                                    @NotNull EnumSet<JobId.OptFields> optFields) {
        return createAndSubmitJob(psm.getProjectSpaceManager(), commandList, alignFeatureIds, toImport, optFields);
    }

    @Override
    public JobId createAndSubmitImportJob(SiriusProjectSpaceImpl psm, ImportLocalFilesSubmission jobSubmission,
                                          @NotNull EnumSet<JobId.OptFields> optFields) {
        return createAndSubmitImportJob(psm.getProjectSpaceManager(), jobSubmission, optFields);
    }

    @Override
    public JobId createAndSubmitImportJob(SiriusProjectSpaceImpl psm, ImportStringSubmission jobSubmission,
                                          @NotNull EnumSet<JobId.OptFields> optFields) {
        return createAndSubmitImportJob(psm.getProjectSpaceManager(), jobSubmission, optFields);
    }

    @Override
    public JobId deleteJob(@Nullable SiriusProjectSpaceImpl psm, String jobId, boolean cancelIfRunning, boolean awaitDeletion, @NotNull EnumSet<JobId.OptFields> optFields) {
        return deleteJob(psm.getProjectSpaceManager(), jobId, cancelIfRunning, awaitDeletion, optFields);
    }

    public BackgroundRuns.BackgroundRunJob<?, ?> getJob(@Nullable SiriusProjectSpaceImpl psm, String jobId) {
        return getJob(psm.getProjectSpaceManager(), jobId);
    }

    @Override
    public JobId getJob(@Nullable SiriusProjectSpaceImpl psm, String jobId, @NotNull EnumSet<JobId.OptFields> optFields) {
        return getJob(psm.getProjectSpaceManager(), jobId, optFields);
    }

    public JobId getJob(@Nullable ProjectSpaceManager<?> psm, String jobId, @NotNull EnumSet<JobId.OptFields> optFields) {
        return extractJobId(getJob(psm, jobId), optFields);
    }

    @Override
    public Page<JobId> getJobs(@Nullable SiriusProjectSpaceImpl psm, @NotNull Pageable pageable, @NotNull EnumSet<JobId.OptFields> optFields) {
        return getJobs(psm.getProjectSpaceManager(), pageable, optFields);
    }

    public Page<JobId> getJobs(@Nullable ProjectSpaceManager<?> psm, @NotNull Pageable pageable, @NotNull EnumSet<JobId.OptFields> optFields) {
        long size = BackgroundRuns.getActiveRunIdMap().values().stream()
                .filter(j -> psm == null || psm.equals(j.getProject())).count();
        return new PageImpl<>(BackgroundRuns.getActiveRunIdMap().values().stream()
                .filter(j -> psm == null || psm.equals(j.getProject()))
                .skip(pageable.getOffset()).limit(pageable.getPageSize())
                .map(j -> extractJobId(j, optFields))
                .toList(), pageable, size);
    }

    @Override
    public JJob<?> getJJob(String jobId) {
        return BackgroundRuns.getActiveRunIdMap().get(Integer.parseInt(jobId));
    }

    @Override
    public void destroy() {
        System.out.println("Destroy Compute Service...");
        if (BackgroundRuns.hasActiveComputations()) {
            log.info("Cancelling running Background Jobs...");
            BackgroundRuns.getActiveRuns().iterator().forEachRemaining(JJob::cancel);
        }
        System.out.println("Destroy Compute Service DONE");
    }
}
