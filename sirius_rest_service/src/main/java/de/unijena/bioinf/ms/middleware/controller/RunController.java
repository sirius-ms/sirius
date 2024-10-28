/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;

import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.removeNone;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/runs")
@Tag(name = "Runs (EXPERIMENTAL)", description = "This API allows accessing LC/MS runs." +
        "All Endpoints are experimental and not part of the stable API specification. " +
        "These endpoints can change at any time, even in minor updates.")
public class RunController extends TagController<Run, Run.OptField> {

    @Autowired
    public RunController(ProjectsProvider<?> projectsProvider) {
        super(projectsProvider);
    }

    @Override
    protected Class<Run> getTaggable() {
        return Run.class;
    }

    /**
     * Get all available runs in the given project-space.
     *
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return Runs with tags (if specified).
     */
    @GetMapping(value = "/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<Run> getRunsPaged(
            @PathVariable String projectId,
            @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "") EnumSet<Run.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId).findRuns(pageable, removeNone(optFields));
    }

    /**
     * Get run with the given identifier from the specified project-space.
     *
     * @param projectId        project-space to read from.
     * @param runId            identifier of run to access.
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return Run with tags (if specified).
     */
    @GetMapping(value = "/{runId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Run getRun(
            @PathVariable String projectId, @PathVariable String runId,
            @RequestParam(defaultValue = "") EnumSet<Run.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId).findRunById(runId, removeNone(optFields));
    }

}
