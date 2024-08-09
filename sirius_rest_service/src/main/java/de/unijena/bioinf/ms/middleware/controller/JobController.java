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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.frontend.core.Workspace;
import de.unijena.bioinf.ms.middleware.configuration.GlobalConfig;
import de.unijena.bioinf.ms.middleware.model.compute.CommandSubmission;
import de.unijena.bioinf.ms.middleware.model.compute.Job;
import de.unijena.bioinf.ms.middleware.model.compute.JobSubmission;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.removeNone;

@RestController
@RequestMapping(value = "/api")
@Tag(name = "Jobs", description = "Start, monitor and cancel background jobs.")
public class JobController {
    public final static String DEFAULT_PARAMETERS = "DEFAULT";
    private final ComputeService computeService;
    private final ProjectsProvider<?> projectsProvider;
    private final GlobalConfig globalConfig;

    public JobController(ComputeService computeService, ProjectsProvider<?> projectsProvider, GlobalConfig globalConfig) {
        this.computeService = computeService;
        this.projectsProvider = projectsProvider;
        this.globalConfig = globalConfig;
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
                                  @RequestParam(defaultValue = "") EnumSet<Job.OptField> optFields
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
                             @RequestParam(defaultValue = "") EnumSet<Job.OptField> optFields
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
        final JobSubmission js = getJobConfig(jobConfigName, true);
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
     *
     * DEPRECATED: this endpoint is based on local file paths and will likely be removed in future versions of this API.
     */
    @Deprecated
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
     * @param cancelIfRunning If true job will be canceled if it is not finished. Otherwise,
     *                        deletion will fail for running jobs or request will block until job has finished.
     * @param awaitDeletion   If true request will block until deletion succeeded or failed.
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
        computeService.deleteJob(projectsProvider.getProjectOrThrow(projectId), jobId, cancelIfRunning, awaitDeletion,
                EnumSet.noneOf(Job.OptField.class));
    }

    /**
     * Request default job configuration
     *
     * @param includeConfigMap if true, generic configmap with-defaults will be included
     * @return {@link JobSubmission} with all parameters set to default values.
     */
    @GetMapping(value = "/default-job-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public JobSubmission getDefaultJobConfig(@RequestParam(required = false, defaultValue = "false") boolean includeConfigMap) {
        return JobSubmission.createDefaultInstance(includeConfigMap);
    }

    /**
     * Request all available job configurations
     *
     * @param includeConfigMap if true the generic configmap will be part of the output
     * @return list of available {@link JobSubmission}s
     */
    @GetMapping(value = "/job-configs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<JobSubmission> getJobConfigs(@RequestParam(required = false, defaultValue = "false") boolean includeConfigMap) {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            List<JobSubmission> js = FileUtils.listAndClose(Workspace.runConfigDir, s -> s.filter(Files::isRegularFile).map(config -> {
                try (InputStream inputStream = Files.newInputStream(config)) {
                    return mapper.readValue(inputStream, JobSubmission.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList()));

            if (!includeConfigMap) js.forEach(j -> j.setConfigMap(null));

            return js;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Error when parsing job-config files.", e.getCause());
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Error.", e.getCause());
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Error when crawling job-config files.");
        }
    }


    /**
     * Request job configuration with given name.
     *
     * @param name             name of the job-config to return
     * @param includeConfigMap if true the generic configmap will be part of the output
     * @return {@link JobSubmission} for given name.
     */
    @GetMapping(value = "/job-configs/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public JobSubmission getJobConfig(@PathVariable @NotNull String name, @RequestParam(required = false, defaultValue = "false") boolean includeConfigMap) {
        if (name.equals(DEFAULT_PARAMETERS)) return getDefaultJobConfig(includeConfigMap);

        final Path config = Workspace.runConfigDir.resolve(name + ".json");
        if (Files.notExists(config) || !Files.isRegularFile(config))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job-config with name '" + name + "' does not exist.");

        try (InputStream s = Files.newInputStream(config)) {
            JobSubmission js = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(s, JobSubmission.class);
            if (!includeConfigMap) js.setConfigMap(null);
            return js;

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error when reading job-config file '" + config + "'.", e);

        }
    }

    /**
     * Add new job configuration with given name.
     *
     * @param name      name of the job-config to add
     * @param jobConfig to add
     * @return Probably modified name of the config (to ensure filesystem path compatibility).
     */
    @PostMapping(value = "/job-configs/{name}", produces = MediaType.TEXT_PLAIN_VALUE) //this needs to be text because some SDKs consider a string field as invalid json.
    @ResponseStatus(HttpStatus.OK)
    public String saveJobConfig(@PathVariable String name, @RequestBody JobSubmission jobConfig, @RequestParam(required = false, defaultValue = "false") boolean overrideExisting) {
        name = name.replaceAll("\\W+", "_");
        if (name.equals(DEFAULT_PARAMETERS))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The job-config name '" + DEFAULT_PARAMETERS + "' is already blocked by the default job-config.");

        final Path config = Workspace.runConfigDir.resolve(name + ".json");
        if (!overrideExisting && Files.exists(config))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job-config with name '" + name + "' already exists. Try again with 'overrideExisting' if you wish to replace it.");

        // remove compoundIds since they are not permitted in a template config
        jobConfig.setCompoundIds(null);

        try (OutputStream s = Files.newOutputStream(config)) {
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(s, jobConfig);
            return name;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Error when reading default config file.", e);
        }
    }

    /**
     * Delete job configuration with given name.
     *
     * @param name name of the job-config to delete
     */
    @DeleteMapping(value = "/job-configs/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteJobConfig(@PathVariable String name) throws IOException {
        Files.deleteIfExists(Workspace.runConfigDir.resolve(name + ".json"));
    }
}
