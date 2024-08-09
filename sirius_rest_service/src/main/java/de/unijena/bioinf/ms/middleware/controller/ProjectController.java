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

import de.unijena.bioinf.babelms.inputresource.PathInputResource;
import de.unijena.bioinf.ms.middleware.model.MultipartInputResource;
import de.unijena.bioinf.ms.middleware.model.compute.ImportLocalFilesSubmission;
import de.unijena.bioinf.ms.middleware.model.compute.ImportMultipartFilesSubmission;
import de.unijena.bioinf.ms.middleware.model.compute.Job;
import de.unijena.bioinf.ms.middleware.model.compute.LcmsSubmissionParameters;
import de.unijena.bioinf.ms.middleware.model.projects.ImportResult;
import de.unijena.bioinf.ms.middleware.model.projects.ProjectInfo;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.removeNone;

@RestController
@RequestMapping(value = "/api/projects")
@Tag(name = "Projects", description = "Manage SIRIUS projects.")
@Slf4j
public class ProjectController {
    private final ComputeService computeService;
    private final ProjectsProvider<?> projectsProvider;

    @Autowired
    public ProjectController(ComputeService computeService, ProjectsProvider<?> projectsProvider) {
        this.computeService = computeService;
        this.projectsProvider = projectsProvider;
    }

    /**
     * List opened project spaces.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProjectInfo> getProjectSpaces() {
        return projectsProvider.listAllProjectSpaces();
    }

    /**
     * Get project space info by its projectId.
     *
     * @param projectId unique name/identifier tof the project-space to be accessed.
     */
    @GetMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectInfo getProjectSpace(@PathVariable String projectId, @RequestParam(defaultValue = "") EnumSet<ProjectInfo.OptField> optFields) {
        return projectsProvider.getProjectInfoOrThrow(projectId, removeNone(optFields));
    }

    /**
     * Open an existing project-space and make it accessible via the given projectId.
     *
     * @param projectId     unique name/identifier that shall be used to access the opened project-space. Must consist only of [a-zA-Z0-9_-].
     * @param pathToProject local file path to open the project from. If NULL, project will be loaded by it projectId from default project location.  DEPRECATED: This parameter relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
     */
    @PutMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectInfo openProjectSpace(@PathVariable String projectId,
                                        @Deprecated @RequestParam(required = false) String pathToProject,
                                        @RequestParam(defaultValue = "") EnumSet<ProjectInfo.OptField> optFields
    ) throws IOException {
        return projectsProvider.openProject(projectId, pathToProject, removeNone(optFields));
    }

    /**
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     *
     * @param projectId     unique name/identifier that shall be used to access the newly created project-space. Must consist only of [a-zA-Z0-9_-].
     * @param pathToProject local file path where the project will be created. If NULL, project will be stored by its projectId in default project location. DEPRECATED: This parameter relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
     */
    @PostMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectInfo createProjectSpace(@PathVariable String projectId,
                                          @Deprecated @RequestParam(required = false) String pathToProject,
                                          @RequestParam(defaultValue = "") EnumSet<ProjectInfo.OptField> optFields
    ) throws IOException {
        return projectsProvider.createProject(projectId, pathToProject, removeNone(optFields));
    }

    /**
     * Close project-space and remove it from application. Project will NOT be deleted from disk.
     * <p>
     * ATTENTION: This will cancel and remove all jobs running on this Project before closing it.
     * If there are many jobs, this might take some time.
     *
     * @param projectId unique name/identifier of the  project-space to be closed.
     */
    @DeleteMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void closeProjectSpace(@PathVariable String projectId) throws Throwable {
        Project<?> ps = projectsProvider.getProject(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT,
                        "Project space with identifier '" + projectId + "' not found!"));
        computeService.deleteJobs(ps, true, true, true, EnumSet.noneOf(Job.OptField.class));
        //todo check if we can make wait for deletion aync
        projectsProvider.closeProjectSpace(projectId);
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project as background job.
     * Possible formats (mzML, mzXML)
     *
     * @param projectId    Project-space to import into.
     * @param inputFiles   Files to import into project.
     * @param parameters   Parameters for feature alignment and feature finding.
     * @param optFields    Set of optional fields to be included. Use 'none' only to override defaults.
     * @return the import job.
     */
    @PostMapping(value = "/{projectId}/import/ms-data-files-job", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Job importMsRunDataAsJob(@PathVariable String projectId,
                                    @RequestBody MultipartFile[] inputFiles,
                                    LcmsSubmissionParameters parameters,
                                    @RequestParam(defaultValue = "progress") EnumSet<Job.OptField> optFields
    ) {
        Project<?> p = projectsProvider.getProjectOrThrow(projectId);
        try {
            ImportMultipartFilesSubmission sub = new ImportMultipartFilesSubmission();
            sub.setInputSources(List.of(inputFiles));
            sub.setLcmsParameters(parameters);
            sub.consumeResources(); //consume multipart resource withing this thread because its gone when moved to background thread.
            return computeService.createAndSubmitMsDataImportJob(p, sub, removeNone(optFields));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error when loading lcms data.", e);
        }
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project
     * Possible formats (mzML, mzXML)
     *
     * @param projectId    Project-space to import into.
     * @param inputFiles   Files to import into project.
     * @param parameters   Parameters for feature alignment and feature finding.
     */
    @PostMapping(value = "/{projectId}/import/ms-data-files", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importMsRunData(@PathVariable String projectId,
                                        @RequestBody MultipartFile[] inputFiles,
                                        LcmsSubmissionParameters parameters
    ) {
        ImportMultipartFilesSubmission sub = new ImportMultipartFilesSubmission();
        sub.setInputSources(List.of(inputFiles));
        sub.setLcmsParameters(parameters);
        return projectsProvider.getProjectOrThrow(projectId).importMsRunData(sub);
    }


    /**
     * Import and Align full MS-Runs from various formats into the specified project as background job.
     * Possible formats (mzML, mzXML)
     * <p>
     * ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,
     * not on the system where the client SDK is running.
     * Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)
     * are running on the same host.
     * <p>
     * DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this
     * API to allow for more flexible use cases. Use 'ms-data-files-job' instead.
     *
     * @param projectId      Project-space to import into.
     * @param localFilePaths Local files to import into project.
     * @param parameters     Parameters for feature alignment and feature finding.
     * @param optFields      Set of optional fields to be included. Use 'none' only to override defaults.
     * @return the import job.
     */
    @Deprecated(forRemoval = true)
    @PostMapping(value = "/{projectId}/import/ms-data-local-files-job", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Job importMsRunDataAsJobLocally(@PathVariable String projectId,
                                           @RequestBody String[] localFilePaths,
                                           LcmsSubmissionParameters parameters,
                                           @RequestParam(defaultValue = "progress") EnumSet<Job.OptField> optFields
    ) {
        Project<?> p = projectsProvider.getProjectOrThrow(projectId);
        try {
            ImportLocalFilesSubmission sub = new ImportLocalFilesSubmission();
            sub.setInputSources(List.of(localFilePaths));
            sub.setLcmsParameters(parameters);
            return computeService.createAndSubmitMsDataImportJob(p, sub, removeNone(optFields));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error when loading lcms data.", e);
        }
    }

    /**
     * Import and Align full MS-Runs from various formats into the specified project
     * Possible formats (mzML, mzXML)
     * <p>
     * ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,
     * not on the system where the client SDK is running.
     * Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)
     * are running on the same host.
     * <p>
     * DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this
     * API to allow for more flexible use cases. Use 'ms-data-files' instead.
     *
     * @param projectId      Project to import into.
     * @param localFilePaths Local files to import into project.
     * @param parameters     Parameters for feature alignment and feature finding.
     */
    @Deprecated(forRemoval = true)
    @PostMapping(value = "/{projectId}/import/ms-local-data-files", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ImportResult importMsRunDataLocally(@PathVariable String projectId,
                                               @RequestBody String[] localFilePaths,
                                               LcmsSubmissionParameters parameters
    ) {
        ImportLocalFilesSubmission sub = new ImportLocalFilesSubmission();
        sub.setInputSources(List.of(localFilePaths));
        sub.setLcmsParameters(parameters);
        return projectsProvider.getProjectOrThrow(projectId).importMsRunData(sub);
    }


    /**
     * Import ms/ms data from the given format into the specified project-space as background job.
     * Possible formats (ms, mgf, cef, msp)
     *
     * @param projectId project-space to import into.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return the import job.
     */
    @PostMapping(value = "/{projectId}/import/preprocessed-data-files-job", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Job importPreprocessedDataAsJob(@PathVariable String projectId,
                                           @RequestBody MultipartFile[] inputFiles,
                                           @RequestParam(defaultValue = "false") boolean ignoreFormulas,
                                           @RequestParam(defaultValue = "true") boolean allowMs1Only,
                                           @RequestParam(defaultValue = "progress") EnumSet<Job.OptField> optFields
    ) {

        Project<?> p = projectsProvider.getProjectOrThrow(projectId);
        ImportMultipartFilesSubmission sub = new ImportMultipartFilesSubmission();
        sub.setInputSources(List.of(inputFiles));
        sub.setIgnoreFormulas(ignoreFormulas);
        sub.setAllowMs1OnlyData(allowMs1Only);
        return computeService.createAndSubmitPeakListImportJob(p, sub, removeNone(optFields));
    }

    /**
     * Import already preprocessed ms/ms data from various formats into the specified project
     * Possible formats (ms, mgf, cef, msp)
     *
     * @param projectId  project-space to import into.
     * @param inputFiles files to import into project
     */
    @PostMapping(value = "/{projectId}/import/preprocessed-data-files", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importPreprocessedData(@PathVariable String projectId,
                                               @RequestBody MultipartFile[] inputFiles,
                                               @RequestParam(defaultValue = "false") boolean ignoreFormulas,
                                               @RequestParam(defaultValue = "true") boolean allowMs1Only
    ) {
        return projectsProvider.getProjectOrThrow(projectId).importPreprocessedData(
                Arrays.stream(inputFiles).map(MultipartInputResource::new).collect(Collectors.toList()),
                ignoreFormulas, allowMs1Only
        );
    }

    /**
     * Import ms/ms data from the given format into the specified project-space as background job.
     * Possible formats (ms, mgf, cef, msp)
     * <p>
     * ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,
     * not on the system where the client SDK is running.
     * Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)
     * are running on the same host.
     * <p>
     * DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this
     * API to allow for more flexible use cases. Use 'preprocessed-data-files-job' instead.
     *
     * @param projectId project-space to import into.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return the import job.
     */
    @PostMapping(value = "/{projectId}/import/preprocessed-local-data-files-job", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Deprecated(forRemoval = true)
    public Job importPreprocessedDataAsJobLocally(@PathVariable String projectId,
                                           @RequestBody String[] localPaths,
                                           @RequestParam(defaultValue = "false") boolean ignoreFormulas,
                                           @RequestParam(defaultValue = "true") boolean allowMs1Only,
                                           @RequestParam(defaultValue = "progress") EnumSet<Job.OptField> optFields
    ) {
        Project<?> p = projectsProvider.getProjectOrThrow(projectId);
        ImportLocalFilesSubmission sub = new ImportLocalFilesSubmission();
        sub.setInputSources(List.of(localPaths));
        sub.setIgnoreFormulas(ignoreFormulas);
        sub.setAllowMs1OnlyData(allowMs1Only);
        return computeService.createAndSubmitPeakListImportJob(p, sub, removeNone(optFields));
    }

    /**
     * Import already preprocessed ms/ms data from various formats into the specified project
     * Possible formats (ms, mgf, cef, msp)
     * <p>
     * ATTENTION: This is loading input files from the filesystem where the SIRIUS service is running,
     * not on the system where the client SDK is running.
     * Is more efficient than MultipartFile upload in cases where client (SDK) and server (SIRIUS service)
     * are running on the same host.
     * <p>
     * DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this
     * API to allow for more flexible use cases. Use 'preprocessed-data-files' instead.
     *
     * @param projectId      project-space to import into.
     * @param localFilePaths files to import into project
     */
    @PostMapping(value = "/{projectId}/import/preprocessed-local-data-files", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Deprecated(forRemoval = true)
    public ImportResult importPreprocessedDataLocally(@PathVariable String projectId,
                                               @RequestBody String[] localFilePaths,
                                               @RequestParam(defaultValue = "false") boolean ignoreFormulas,
                                               @RequestParam(defaultValue = "true") boolean allowMs1Only
    ) {
        return projectsProvider.getProjectOrThrow(projectId).importPreprocessedData(
                Arrays.stream(localFilePaths).map(Path::of).map(PathInputResource::new).collect(Collectors.toList()),
                ignoreFormulas, allowMs1Only
        );
    }


    /**
     * Move an existing (opened) project-space to another location.
     *
     * @param projectId           unique name/identifier of the project-space that shall be copied.
     * @param pathToCopiedProject target location where the source project will be copied to.
     * @param copyProjectId       optional id/mame of the newly created project (copy). If given the project will be opened.
     * @return ProjectInfo of the newly created project if opened (copyProjectId != null) or the project info of
     * the source project otherwise
     * <p>
     * DEPRECATED: This endpoint relies on the local filesystem and will likely be removed in later versions of this API to allow for more flexible use cases.
     */
    @Deprecated
    @PutMapping(value = "/{projectId}/copy", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectInfo copyProjectSpace(@PathVariable String projectId, @RequestParam String pathToCopiedProject, @RequestParam(required = false) String copyProjectId, @RequestParam(defaultValue = "") EnumSet<ProjectInfo.OptField> optFields) throws IOException {
        return projectsProvider.copyProject(projectId, pathToCopiedProject, copyProjectId, removeNone(optFields));
    }

    @Operation(summary = "Get CSI:FingerID fingerprint (prediction vector) definition")
    @GetMapping(value = {"/{projectId}/fingerid-data"}, produces = "application/CSV")
    @ResponseStatus(HttpStatus.OK)
    public String getFingerIdData(@PathVariable String projectId, @RequestParam int charge) {
        return projectsProvider.getProjectOrThrow(projectId).getFingerIdDataCSV(charge);
    }

    @Operation(summary = "Get CANOPUS prediction vector definition for ClassyFire classes")
    @GetMapping(value = {"/{projectId}/cf-data"}, produces = "application/CSV")
    @ResponseStatus(HttpStatus.OK)
    public String getCanopusClassyFireData(@PathVariable String projectId, @RequestParam int charge) {
        return projectsProvider.getProjectOrThrow(projectId).getCanopusClassyFireDataCSV(charge);
    }

    @Operation(summary = "Get CANOPUS prediction vector definition for NPC classes")
    @GetMapping(value = {"/{projectId}/npc-data"}, produces = "application/CSV")
    @ResponseStatus(HttpStatus.OK)
    public String getCanopusNpcData(@PathVariable String projectId, @RequestParam int charge) {
        return projectsProvider.getProjectOrThrow(projectId).getCanopusNpcDataCSV(charge);
    }
}
