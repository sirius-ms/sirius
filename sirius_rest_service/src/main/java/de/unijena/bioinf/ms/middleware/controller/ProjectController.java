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

package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.middleware.model.compute.Job;
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
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping(value = "/api/projects")
@Tag(name = "Projects", description = "Manage SIRIUS projects.")
@Slf4j
public class ProjectController {
    private final ComputeService computeContext;
    private final ProjectsProvider projectsProvider;

    @Autowired
    public ProjectController(ComputeService<?> context, ProjectsProvider<?> projectsProvider) {
        this.computeContext = context;
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
        return projectsProvider.getProjectInfoOrThrow(projectId, optFields);
    }

    /**
     * Open an existing project-space and make it accessible via the given projectId.
     *
     * @param projectId unique name/identifier that shall be used to access the opened project-space. Must consist only of [a-zA-Z0-9_-].
     */
    @PutMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectInfo openProjectSpace(@PathVariable String projectId,
                                        @RequestParam String pathToProject,
                                        @RequestParam(defaultValue = "") EnumSet<ProjectInfo.OptField> optFields) throws IOException {
        try {
            return projectsProvider.openProjectSpace(projectId, pathToProject, optFields);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     *
     * @param projectId unique name/identifier that shall be used to access the newly created project-space. Must consist only of [a-zA-Z0-9_-].
     */
    @PostMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectInfo createProjectSpace(@PathVariable String projectId,
                                          @RequestParam String pathToProject,
                                          @RequestParam(required = false) String pathToSourceProject,
                                          @RequestParam(required = false, defaultValue = "true") boolean awaitImport
    ) throws IOException {
        InputFilesOptions inputFiles = null;
        if (pathToSourceProject != null) {
            inputFiles = new InputFilesOptions();
            inputFiles.msInput = new InputFilesOptions.MsInput();
            inputFiles.msInput.setAllowMS1Only(true);
            inputFiles.msInput.setInputPath(List.of(Path.of(pathToSourceProject)));

            if (!inputFiles.msInput.isSingleProject())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported input! 'pathToSourceProject' needs to point to a valid SIRIUS project-space");

        }

        ProjectInfo pid = null;
        try {
            pid = projectsProvider.createProjectSpace(projectId, Path.of(pathToProject));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        de.unijena.bioinf.ms.middleware.service.projects.Project project = projectsProvider.getProjectOrThrow(projectId);
        if (inputFiles != null) {
            Job id = computeContext.createAndSubmitJob(project, List.of("project-space", "--keep-open"),
                    null, inputFiles, EnumSet.allOf(Job.OptField.class));
            if (awaitImport) { //todo maybe separate endpoint for non waiting.
                try {
                    computeContext.getJJob(project, id.getId()).awaitResult();
                    computeContext.deleteJob(project, id.getId(), false, true, EnumSet.noneOf(Job.OptField.class));
                } catch (ExecutionException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error when waiting for import jobs '" + id.getId() + "'.", e);
                }
            }
        }
        return pid;
    }

    /**
     * Close project-space and remove it from application. Project will NOT be deleted from disk.
     *
     * ATTENTION: This will cancel and remove all jobs running on this Project before closing it.
     * If there are many jobs, this might take some time.
     *
     * @param projectId unique name/identifier of the  project-space to be closed.
     */
    @DeleteMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void closeProjectSpace(@PathVariable String projectId) throws Throwable {
        Project ps = (Project) projectsProvider.getProject(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT,
                        "Project space with identifier '" + projectId + "' not found!"));
        computeContext.deleteJobs(ps, true, true, true, EnumSet.noneOf(Job.OptField.class));
        //todo check if we can make wait for deletion aync
        projectsProvider.closeProjectSpace(projectId);
    }

    /**
     * Move an existing (opened) project-space to another location.
     *
     * @param projectId unique name/identifier of the project-space that shall be copied.
     * @param pathToCopiedProject target location where the source project will be copied to.
     * @param copyProjectId optional id/mame of the newly created project (copy). If given the project will be opened.
     * @return ProjectInfo of the newly created project if opened (copyProjectId != null) or the project info of
     * the source project otherwise
     */
    @PutMapping(value = "/{projectId}/copy", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectInfo copyProjectSpace(@PathVariable String projectId, @RequestParam String pathToCopiedProject, @RequestParam(required = false) String copyProjectId, @RequestParam(defaultValue = "") EnumSet<ProjectInfo.OptField> optFields) throws IOException {
        return projectsProvider.copyProjectSpace(projectId, copyProjectId, pathToCopiedProject, optFields);
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
