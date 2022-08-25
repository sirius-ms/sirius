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

package de.unijena.bioinf.ms.middleware.compute;

import de.unijena.bioinf.ms.middleware.BaseApiController;
import de.unijena.bioinf.ms.middleware.compute.model.ComputeContext;
import de.unijena.bioinf.ms.middleware.compute.model.JobId;
import de.unijena.bioinf.ms.middleware.compute.model.JobSubmission;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api")
@Tag(name = "Computations", description = "Start, monitor and cancel compute jobs.")
public class ComputeController extends BaseApiController {
    ComputeContext computeContext;

    public ComputeController(ComputeContext computeContext) {
        super(computeContext.siriusContext);
        this.computeContext = computeContext;
    }


    /**
     * Get job information and its current state and progress (if available).
     *
     * @param projectId      project-space to run jobs on
     * @param includeState   include {@link de.unijena.bioinf.ms.middleware.compute.model.JobProgress} states.
     * @param includeCommand include job commands.
     * @param includeAffectedCompounds include list of compound ids affected by this job (if available)
     */

    @GetMapping(value = "/projects/{projectId}/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<JobId> getJobs(@PathVariable String projectId,
                               @RequestParam(required = false, defaultValue = "false") boolean includeState,
                               @RequestParam(required = false, defaultValue = "false") boolean includeCommand,
                               @RequestParam(required = false, defaultValue = "false") boolean includeAffectedCompounds
    ) {
        return computeContext.getJobs(projectSpace(projectId), includeState, includeCommand, includeAffectedCompounds);
    }

    /**
     * Get job information and its current state and progress (if available).
     *
     * @param projectId      project-space to run jobs on
     * @param jobId          of the job to be returned
     * @param includeState   include {@link de.unijena.bioinf.ms.middleware.compute.model.JobProgress} state.
     * @param includeCommand include job command.
     * @param includeAffectedCompounds include list of compound ids affected by this job (if available)
     */
    @GetMapping(value = "/projects/{projectId}/jobs/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public JobId getJob(@PathVariable String projectId, @PathVariable String jobId,
                        @RequestParam(required = false, defaultValue = "true") boolean includeState,
                        @RequestParam(required = false, defaultValue = "false") boolean includeCommand,
                        @RequestParam(required = false, defaultValue = "false") boolean includeAffectedCompounds
    ) {
        return computeContext.getJob(projectSpace(projectId), jobId, includeState, includeCommand, includeAffectedCompounds);
    }

    /**
     * Start computation for given compounds and with given parameters.
     *
     * @param projectId      project-space to run jobs on
     * @param jobSubmission  configuration of the job that will be submitted of the job to be returned
     * @param includeState   include {@link de.unijena.bioinf.ms.middleware.compute.model.JobProgress} state.
     * @param includeCommand include job command.
     * @param includeAffectedCompounds include list of compound ids affected by this job (if available)
     */
    @PostMapping(value = "/projects/{projectId}/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public JobId startJob(@PathVariable String projectId, @RequestBody JobSubmission jobSubmission,
                          @RequestParam(required = false, defaultValue = "true") boolean includeState,
                          @RequestParam(required = false, defaultValue = "true") boolean includeCommand,
                          @RequestParam(required = false, defaultValue = "false") boolean includeAffectedCompounds
    ) {
        return computeContext.createAndSubmitJob(projectSpace(projectId), jobSubmission, includeState, includeCommand, includeAffectedCompounds);
    }

    /**
     * Delete job. Specify how to behave for running jobs.
     *
     * @param projectId       project-space to run jobs on
     * @param jobId           of the job to be deleted
     * @param cancelIfRunning If true job will be canceled if it is not finished. Otherwise,
     *                        deletion will fail for running jobs or request will block until job has finished.
     * @param awaitDeletion   If true request will block until deletion succeeded or failed.
     *                        If the job is still running the request will wait until the job has finished.
     */
    @DeleteMapping(value = "/projects/{projectId}/jobs/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteJob(@PathVariable String projectId,
                          @PathVariable String jobId,
                          @RequestParam(required = false, defaultValue = "true") boolean cancelIfRunning,
                          @RequestParam(required = false, defaultValue = "true") boolean awaitDeletion) {
        computeContext.deleteJob(projectSpace(projectId), jobId, false, false, false, cancelIfRunning, awaitDeletion);
    }

    /**
     * Get with all parameters set to default values.
     */
    @GetMapping(value = "/default-job-parameters", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public JobSubmission getDefaultJobParameters(@RequestParam(required = false, defaultValue = "false") boolean includeConfigMap) {
        return JobSubmission.createDefaultInstance(includeConfigMap);
    }
}
