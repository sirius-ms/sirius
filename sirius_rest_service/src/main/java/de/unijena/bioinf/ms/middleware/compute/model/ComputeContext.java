/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.compute.model;

import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobStateEvent;
import de.unijena.bioinf.ms.frontend.BackgroundRuns;
import de.unijena.bioinf.ms.middleware.SiriusContext;
import de.unijena.bioinf.ms.middleware.compute.model.tools.Tool;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Configuration
public class ComputeContext {

    public final SiriusContext siriusContext;


    public ComputeContext(SiriusContext siriusContext) {
        this.siriusContext = siriusContext;
    }

    public <I extends Instance, P extends ProjectSpaceManager<I>> JobId createAndSubmitJob(P psm, JobSubmission jobSubmission, boolean progress, boolean command) {
        List<CompoundContainerId> compounds = new ArrayList<>(jobSubmission.compoundIds.size());
        for (String cid : jobSubmission.compoundIds) {
            compounds.add(psm.projectSpace().findCompound(cid).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NO_CONTENT, "Compound with id '" + cid + "' does not exist!'. No job has been started!")));
        }

        try {
            List<String> commandList = makeCommand(jobSubmission);
            BackgroundRuns.BackgroundRunJob<P, I> run = BackgroundRuns.runCommand(commandList, compounds, psm);
            return extractJobId(run, progress, command);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create Job Command!", e);
        }
    }

    public JobId deleteJob(String jobId, boolean progress, boolean command, boolean cancelIfRunning, boolean awaitDeletion) {
        return deleteJob(null, jobId, progress, command, cancelIfRunning, awaitDeletion);
    }

    public JobId deleteJob(@Nullable ProjectSpaceManager<?> psm, String jobId, boolean progress, boolean command, boolean cancelIfRunning, boolean awaitDeletion) {
        BackgroundRuns.BackgroundRunJob<?, ?> j = getJob(psm, jobId);
        if (j.isFinished()) {
            BackgroundRuns.removeRun(j.getRunId());
        } else {
            if (cancelIfRunning)
                j.cancel();
            if (awaitDeletion) {
                j.getResult();
            } else {
                j.addPropertyChangeListener(JobStateEvent.JOB_STATE_EVENT, evt -> {
                    if (evt instanceof JobStateEvent) {
                        final BackgroundRuns.BackgroundRunJob jj = (BackgroundRuns.BackgroundRunJob) evt.getSource();
                        if (jj.isFinished())
                            BackgroundRuns.removeRun(jj.getRunId());
                    }
                });
            }
        }
        return extractJobId(j, progress, command);
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

    public JobId getJob(@Nullable ProjectSpaceManager<?> psm, String jobId, boolean progress, boolean command) {
        return extractJobId(getJob(psm, jobId), progress, command);
    }

    public List<JobId> getJobs(boolean progress, boolean command) {
        return getJobs(null, progress, command);
    }

    public List<JobId> getJobs(@Nullable ProjectSpaceManager<?> psm, boolean progress, boolean command) {
        return BackgroundRuns.getActiveRunIdMap().values().stream()
                .filter(j -> psm == null || psm.equals(j.getProject()))
                .map(j -> extractJobId(j, progress, command))
                .collect(Collectors.toList());
    }


    protected JobId extractJobId(BackgroundRuns.BackgroundRunJob<?, ?> runJob, boolean progress, boolean command) {
        JobId id = new JobId();
        id.setId(String.valueOf(runJob.getRunId()));
        if (command)
            id.setCommand(runJob.getCommand());
        if (progress)
            id.setProgress(extractProgress(runJob));

        return id;
    }

    protected JobProgress extractProgress(BackgroundRuns.BackgroundRunJob<?, ?> runJob) {
        JobProgress p = new JobProgress();
        p.setState(runJob.getState());

        JobProgressEvent evt = runJob.currentProgress();
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

    protected List<String> makeCommand(JobSubmission jobSubmission) {
        ArrayList<String> commands = new ArrayList<>(makeConfigToolCommand(jobSubmission));
        commands.addAll(jobSubmission.getEnabledTools().stream().map(Tool::getCommand).map(CommandLine.Command::name)
                .collect(Collectors.toList()));
        return commands;
    }

    protected List<String> makeConfigToolCommand(JobSubmission jobSubmission) {
        List<String> configTool = new ArrayList<>();
        configTool.add("config");
        makeCombinedConfigMap(jobSubmission).forEach((k, v) -> {
            configTool.add("--" + k);
            configTool.add(v);
        });

        return configTool;
    }

    protected Map<String, String> makeCombinedConfigMap(JobSubmission jobSubmission) {
        Map<String, String> combined = new HashMap<>(jobSubmission.getConfigMap());
        if (jobSubmission.getEnforcedAdducts() != null)
            combined.put("AdductSettings.enforced", String.join(",", jobSubmission.getEnforcedAdducts()));

        if (jobSubmission.getDetectableAdducts() != null)
            combined.put("AdductSettings.detectable", String.join(",", jobSubmission.getDetectableAdducts()));

        if (jobSubmission.getFallbackAdducts() != null)
            combined.put("AdductSettings.fallback", String.join(",", jobSubmission.getFallbackAdducts()));

        jobSubmission.getEnabledTools().stream().map(Tool::asConfigMap).forEach(combined::putAll);

        return combined;
    }
}
