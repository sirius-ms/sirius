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

package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.ms.middleware.configuration.GlobalConfig;
import de.unijena.bioinf.ms.middleware.model.compute.CommandSubmission;
import de.unijena.bioinf.ms.middleware.model.compute.Job;
import de.unijena.bioinf.ms.middleware.model.compute.JobSubmission;
import de.unijena.bioinf.ms.middleware.model.compute.StoredJobSubmission;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.job.JobConfigService;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;

import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.removeNone;

@RestController
@RequestMapping(value = "/api")
@Tag(name = "Jobs", description = "Start, monitor and cancel background jobs.")
public class JobController {
    public final static String DEFAULT_CONFIG_NAME = "Default";
    private final ComputeService computeService;
    private final ProjectsProvider<?> projectsProvider;
    private final GlobalConfig globalConfig;
    private final JobConfigService jobConfigService;

    public JobController(ComputeService computeService, ProjectsProvider<?> projectsProvider, GlobalConfig globalConfig, JobConfigService jobConfigService) {
        this.computeService = computeService;
        this.projectsProvider = projectsProvider;
        this.globalConfig = globalConfig;
        this.jobConfigService = jobConfigService;
    }


    /**
     * Get Page of jobs with information such as current state and progress (if available).
     *
     * @param projectId project-space to run jobs on
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     */
    @GetMapping(value = "/projects/{projectId}/jobs/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Page<Job> getJobsPaged(@PathVariable String projectId,
                                  @ParameterObject Pageable pageable,
                                  @RequestParam(defaultValue = "none") EnumSet<Job.OptField> optFields
    ) {
        return computeService.getJobs(projectsProvider.getProjectOrThrow(projectId), pageable, removeNone(optFields));
    }

    /**
     * Get List of all available jobs with information such as current state and progress (if available).
     *
     * @param projectId project-space to run jobs on
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     */
    @GetMapping(value = "/projects/{projectId}/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<Job> getJobs(@PathVariable String projectId,
                             @RequestParam(defaultValue = "none") EnumSet<Job.OptField> optFields
    ) {
        return getJobsPaged(projectId, globalConfig.unpaged(), optFields).stream().toList();
    }

    @GetMapping(value = "/projects/{projectId}/has-jobs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public boolean hasJobs(@PathVariable String projectId,
                           @RequestParam(defaultValue = "false") boolean includeFinished
    ) {
        return computeService.hasJobs(projectsProvider.getProjectOrThrow(projectId), includeFinished);
    }

    /**
     * Get job information and its current state and progress (if available).
     *
     * @param projectId project-space to run jobs on
     * @param jobId     of the job to be returned
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     */
    @GetMapping(value = "/projects/{projectId}/jobs/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Job getJob(@PathVariable String projectId, @PathVariable String jobId,
                      @RequestParam(defaultValue = "progress") EnumSet<Job.OptField> optFields
    ) {
        return computeService.getJob(projectsProvider.getProjectOrThrow(projectId), jobId, removeNone(optFields));
    }

    /**
     * Start computation for given compounds and with given parameters.
     *
     * @param projectId     project-space to run jobs on
     * @param jobSubmission configuration of the job that will be submitted of the job to be returned
     * @param optFields     set of optional fields to be included. Use 'none' only to override defaults.
     */
    @PostMapping(value = "/projects/{projectId}/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Job startJob(@PathVariable String projectId, @RequestBody JobSubmission jobSubmission,
                        @RequestParam(defaultValue = "command, progress") EnumSet<Job.OptField> optFields
    ) {
        return computeService.createAndSubmitJob(projectsProvider.getProjectOrThrow(projectId), jobSubmission, removeNone(optFields));
    }


    /**
     * Start computation for given compounds and with parameters from a stored job-config.
     *
     * @param projectId     project-space to run jobs on
     * @param jobConfigName name if the config to be used
     * @param alignedFeatureIds  List of alignedFeatureIds to be computed
     * @param recompute     enable or disable recompute. If null the stored value will be used.
     * @param optFields     set of optional fields to be included. Use 'none' only to override defaults.
     */
    @PostMapping(value = "/projects/{projectId}/jobs/from-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Job startJobFromConfig(@PathVariable String projectId, @RequestParam String jobConfigName,
                                  @RequestBody List<String> alignedFeatureIds,
                                  @RequestParam(required = false) @Nullable Boolean recompute,
                                  @RequestParam(defaultValue = "command, progress") EnumSet<Job.OptField> optFields
    ) {
        final JobSubmission js = getJobConfig(jobConfigName, false).getJobSubmission();
        js.setAlignedFeatureIds(alignedFeatureIds);
        if (recompute != null)
            js.setRecompute(recompute);

        return computeService.createAndSubmitJob(projectsProvider.getProjectOrThrow(projectId), js, removeNone(optFields));
    }

    /**
     * Start computation for given command and input.
     *
     * @param projectId         project-space to perform the command for.
     * @param commandSubmission the command and the input to be executed
     * @param optFields         set of optional fields to be included. Use 'none' only to override defaults.
     * @return Job of the command to be executed.
     */
    @Deprecated(forRemoval = true)
    @Operation(
            summary = "DEPRECATED: this endpoint is based on local file paths and will likely be removed in future versions of this API."
    )
    @PostMapping(value = "/projects/{projectId}/jobs/run-command", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Job startCommand(@PathVariable String projectId, @Valid @RequestBody CommandSubmission commandSubmission,
                            @RequestParam(defaultValue = "progress") EnumSet<Job.OptField> optFields
    ) {
        Project<?> p = projectsProvider.getProjectOrThrow(projectId);
        return computeService.createAndSubmitCommandJob(p, commandSubmission, removeNone(optFields));
    }

    /**
     * * Delete ALL jobs. Specify how to behave for running jobs.
     *
     * @param projectId       project-space to delete jobs from
     * @param cancelIfRunning If true, job will be canceled if it is not finished. Otherwise,
     *                        deletion will fail for running jobs or request will block until job has finished.
     * @param awaitDeletion   If true, request will block until deletion succeeded or failed.
     *                        If the job is still running the request will wait until the job has finished.
     */

    @DeleteMapping(value = "/projects/{projectId}/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteJobs(@PathVariable String projectId,
                           @RequestParam(required = false, defaultValue = "true") boolean cancelIfRunning,
                           @RequestParam(required = false, defaultValue = "true") boolean awaitDeletion
    ) {
        computeService.deleteJobs(projectsProvider.getProjectOrThrow(projectId), cancelIfRunning, awaitDeletion,
                false, EnumSet.noneOf(Job.OptField.class));
    }


    /**
     * Delete job. Specify how to behave for running jobs.
     *
     * @param projectId       project-space to delete job from
     * @param jobId           of the job to be deleted
     * @param cancelIfRunning If true, job will be canceled if it is not finished. Otherwise,
     *                        deletion will fail for running jobs or request will block until job has finished.
     * @param awaitDeletion   If true, request will block until deletion succeeded or failed.
     *                        If the job is still running the request will wait until the job has finished.
     */
    @DeleteMapping(value = "/projects/{projectId}/jobs/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteJob(@PathVariable String projectId,
                          @PathVariable String jobId,
                          @RequestParam(required = false, defaultValue = "true") boolean cancelIfRunning,
                          @RequestParam(required = false, defaultValue = "true") boolean awaitDeletion) {
        computeService.deleteJob(projectsProvider.getProjectOrThrow(projectId), jobId, cancelIfRunning, awaitDeletion,
                EnumSet.noneOf(Job.OptField.class));
    }

    /**
     * Request default job configuration
     *
     * @param includeConfigMap if true, generic configmap with-defaults will be included
     * @param moveParametersToConfigMap if true, object-based parameters will be converted to and added to the generic configMap parameters
     * @param includeCustomDbsForStructureSearch if true, default database selection of structure db search contains also all available custom DB.
     * @return {@link JobSubmission} with all parameters set to default values.
     */
    @GetMapping(value = "/default-job-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public JobSubmission getDefaultJobConfig(@RequestParam(required = false, defaultValue = "false") boolean includeConfigMap,
                                             @RequestParam(required = false, defaultValue = "false") boolean moveParametersToConfigMap,
                                             @RequestParam(required = false, defaultValue = "false") boolean includeCustomDbsForStructureSearch) {
        JobSubmission js = jobConfigService.getDefaultJobConfig(includeCustomDbsForStructureSearch);
        if (moveParametersToConfigMap) {
            js.mergeCombinedConfigMap();
        }
        if (!includeConfigMap) {
            js.setConfigMap(null);
        }
        return js;
    }

    /**
     * Request all available job configurations
     *
     * @return list of available {@link JobSubmission}s
     */
    @GetMapping(value = "/job-configs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<StoredJobSubmission> getJobConfigs() {
        List<StoredJobSubmission> configs = jobConfigService.getAllConfigs();
        configs.forEach(js -> js.getJobSubmission().mergeCombinedConfigMap());
        return configs;
    }

    /**
     * Get all (non-default) job configuration names
     */
    @GetMapping(value = "/job-config-names", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Deprecated(forRemoval = true)
    @Operation(
            summary = "DEPRECATED: use /job-configs to get all configs with names."
    )
    public List<String> getJobConfigNames() {
        return getJobConfigs().stream()
                .map(StoredJobSubmission::getName)
                .filter(n -> !n.equals(JobConfigService.DEFAULT_CONFIG_NAME))
                .toList();
    }

    /**
     * Request job configuration with given name.
     *
     * @param name             name of the job-config to return
     * @param moveParametersToConfigMap if true, object-based parameters will be converted to and added to the generic configMap parameters
     * @return {@link JobSubmission} for given name.
     */
    @GetMapping(value = "/job-configs/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public StoredJobSubmission getJobConfig(
            @PathVariable @NotNull String name,
            @RequestParam(required = false, defaultValue = "false") boolean moveParametersToConfigMap
    ) {
        StoredJobSubmission config = jobConfigService.getConfig(name);
        if (moveParametersToConfigMap) {
            config.getJobSubmission().mergeCombinedConfigMap();
        }
        return config;
    }

    /**
     * Add new job configuration with given name.
     *
     * @param name                      name of the job-config to add
     * @param jobConfig                 to add
     * @param moveParametersToConfigMap if true, object-based parameters will be converted to and added to the generic configMap parameters in the return object
     * @return StoredJobSubmission that contains the JobSubmission and the probably modified name of the config (to ensure path compatibility).
     */
    @PostMapping(value = "/job-configs/{name}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public StoredJobSubmission saveJobConfig(
            @PathVariable String name, @RequestBody JobSubmission jobConfig,
            @RequestParam(required = false, defaultValue = "false") boolean overrideExisting,
            @RequestParam(required = false, defaultValue = "false") boolean moveParametersToConfigMap
    ) {
        name = name.replaceAll("\\W+", "_");
        // remove compounds since they are not permitted in a template config
        jobConfig.setCompoundIds(null);
        jobConfig.setAlignedFeatureIds(null);

        if (jobConfigService.configExists(name)) {
            if (overrideExisting) {
                jobConfigService.updateConfig(name, jobConfig);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job-config with name '" + name + "' already exists. Try again with 'overrideExisting' if you wish to replace it.");
            }
        } else {
            jobConfigService.addUserConfig(name, jobConfig);
        }

        return getJobConfig(name, moveParametersToConfigMap);
    }

    /**
     * Delete job configuration with given name.
     *
     * @param name name of the job-config to delete
     */
    @DeleteMapping(value = "/job-configs/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteJobConfig(@PathVariable String name) {
        jobConfigService.deleteUserConfig(name);
    }
}
