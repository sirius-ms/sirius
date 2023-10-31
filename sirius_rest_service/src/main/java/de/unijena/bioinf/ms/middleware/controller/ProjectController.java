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
import de.unijena.bioinf.ms.middleware.model.SearchQueryType;
import de.unijena.bioinf.ms.middleware.model.compute.Job;
import de.unijena.bioinf.ms.middleware.model.projects.ProjectId;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
public class ProjectController {
    //todo add access to fingerprint definitions aka molecular property names
    private final ComputeService computeContext;
    private final ProjectsProvider projectsProvider;

    @Autowired
    public ProjectController(ComputeService<?> context, ProjectsProvider<?> projectsProvider) {
        this.computeContext = context;
        this.projectsProvider = projectsProvider;
    }

    /**
     * List opened project spaces.
     * @param searchQuery optional search query in specified format
     * @param querySyntax query syntax used fpr searchQuery
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<ProjectId> getProjectSpaces(@ParameterObject Pageable pageable,
                                            @RequestParam(required = false) String searchQuery,
                                            @RequestParam(defaultValue = "LUCENE") SearchQueryType querySyntax) {
        final List<ProjectId> all = projectsProvider.listAllProjectSpaces();
        return new PageImpl<>(
                all.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).toList(), pageable, all.size()
        );
    }

    /**
     * Get project space info by its projectId.
     *
     * @param projectId unique name/identifier tof the project-space to be accessed.
     */
    @GetMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectId getProjectSpace(@PathVariable String projectId) {
        //todo add infos like size and number of compounds?
        return projectsProvider.getProjectIdOrThrow(projectId);
    }

    /**
     * Open an existing project-space and make it accessible via the given projectId.
     *
     * @param projectId unique name/identifier that shall be used to access the opened project-space.
     */
    @PutMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectId openProjectSpace(@PathVariable String projectId, @RequestParam String pathToProject) throws IOException {
        return projectsProvider.openProjectSpace(new ProjectId(projectId, pathToProject));
    }

    /**
     * Create and open a new project-space at given location and make it accessible via the given projectId.
     *
     * @param projectId unique name/identifier that shall be used to access the newly created project-space.
     */
    @PostMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectId createProjectSpace(@PathVariable String projectId,
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

        ProjectId pid = projectsProvider.createProjectSpace(projectId, Path.of(pathToProject));
        Project project = projectsProvider.getProjectOrThrow(projectId);
        if (inputFiles != null) {
            Job id = computeContext.createAndSubmitJob(project, List.of("project-space", "--keep-open"),
                    null, inputFiles, EnumSet.allOf(Job.OptField.class));
            if (awaitImport) { //todo maybe separate endpoint for non waiting.
                try {
                    computeContext.getJJob(id.getId()).awaitResult();
                    computeContext.deleteJob(id.getId(), false, true, EnumSet.noneOf(Job.OptField.class));
                } catch (ExecutionException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error when waiting for import jobs '" + id.getId() + "'.", e);
                }
            }
        }
        return pid;
    }

    /**
     * Close project-space and remove it from application. Project-space will NOT be deleted from disk.
     *
     * @param projectId unique name/identifier of the  project-space to be closed.
     */
    @DeleteMapping(value = "/{projectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void closeProjectSpace(@PathVariable String projectId) throws IOException {
        projectsProvider.closeProjectSpace(projectId);
    }
}
