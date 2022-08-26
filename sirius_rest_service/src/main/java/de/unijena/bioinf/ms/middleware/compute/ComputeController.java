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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.frontend.core.Workspace;
import de.unijena.bioinf.ms.middleware.BaseApiController;
import de.unijena.bioinf.ms.middleware.compute.model.ComputeContext;
import de.unijena.bioinf.ms.middleware.compute.model.JobId;
import de.unijena.bioinf.ms.middleware.compute.model.JobSubmission;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api")
@Tag(name = "Computations", description = "Start, monitor and cancel compute jobs.")
public class ComputeController extends BaseApiController {
    public final static String DEFAULT_PARAMETERS = "DEFAULT";
    ComputeContext computeContext;

    public ComputeController(ComputeContext computeContext) {
        super(computeContext.siriusContext);
        this.computeContext = computeContext;
    }


    /**
     * Get job information and its current state and progress (if available).
     *
     * @param projectId                project-space to run jobs on
     * @param includeState             include {@link de.unijena.bioinf.ms.middleware.compute.model.JobProgress} states.
     * @param includeCommand           include job commands.
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
     * @param projectId                project-space to run jobs on
     * @param jobId                    of the job to be returned
     * @param includeState             include {@link de.unijena.bioinf.ms.middleware.compute.model.JobProgress} state.
     * @param includeCommand           include job command.
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
     * @param projectId                project-space to run jobs on
     * @param jobSubmission            configuration of the job that will be submitted of the job to be returned
     * @param includeState             include {@link de.unijena.bioinf.ms.middleware.compute.model.JobProgress} state.
     * @param includeCommand           include job command.
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
     * Start computation for given compounds and with parameters from a stored job-config.
     *
     * @param projectId                project-space to run jobs on
     * @param jobConfigName            name if the config to be used
     * @param compoundIds              compound ids to be computed
     * @param recompute                enable or disable recompute. If null the stored value will be used.
     * @param includeState             include {@link de.unijena.bioinf.ms.middleware.compute.model.JobProgress} state.
     * @param includeCommand           include job command.
     * @param includeAffectedCompounds include list of compound ids affected by this job (if available)
     */
    @PostMapping(value = "/projects/{projectId}/jobs-from-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public JobId startJobFromConfig(@PathVariable String projectId, @RequestParam String jobConfigName, @RequestBody List<String> compoundIds,
                                    @RequestParam(required = false) @Nullable Boolean recompute,
                                    @RequestParam(required = false, defaultValue = "true") boolean includeState,
                                    @RequestParam(required = false, defaultValue = "true") boolean includeCommand,
                                    @RequestParam(required = false, defaultValue = "false") boolean includeAffectedCompounds
    ) {
        final JobSubmission js = getJobConfig(jobConfigName, true);
        js.setCompoundIds(compoundIds);
        if (recompute != null)
            js.setRecompute(recompute);

        return computeContext.createAndSubmitJob(projectSpace(projectId), js, includeState, includeCommand, includeAffectedCompounds);
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
    @PostMapping(value = "/job-configs/{name}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public String postJobConfig(@PathVariable String name, @RequestBody JobSubmission jobConfig, @RequestParam(required = false, defaultValue = "false") boolean overrideExisting) {
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
