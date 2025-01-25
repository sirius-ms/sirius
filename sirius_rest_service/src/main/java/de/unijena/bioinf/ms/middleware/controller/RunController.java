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

import de.unijena.bioinf.ms.middleware.controller.mixins.TaggableController;
import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Getter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.List;

import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.removeNone;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/runs")
@Tag(name = "Runs", description = "[EXPERIMENTAL] This API allows accessing LC/MS runs. " +
        "All endpoints are experimental and not part of the stable API specification. " +
        "These endpoints can change at any time, even in minor updates.")
public class RunController implements TaggableController<Run, Run.OptField> {

    @Getter
    private ProjectsProvider<?> projectsProvider;

    @Autowired
    public RunController(ProjectsProvider<?> projectsProvider) {
        this.projectsProvider = projectsProvider;
    }

    /**
     * [EXPERIMENTAL] Get all available runs in the given project-space.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return Runs with tags (if specified).
     */
    @Operation(operationId = "getRunPageExperimental")
    @GetMapping(value = "/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<Run> getRunsPaged(
            @PathVariable String projectId,
            @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "") EnumSet<Run.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId).findRuns(pageable, removeNone(optFields));
    }

    /**
     * [EXPERIMENTAL] Get run with the given identifier from the specified project-space.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId        project-space to read from.
     * @param runId            identifier of run to access.
     * @param optFields        set of optional fields to be included. Use 'none' only to override defaults.
     * @return Run with tags (if specified).
     */
    @Operation(operationId = "getRunExperimental")
    @GetMapping(value = "/{runId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Run getRun(
            @PathVariable String projectId, @PathVariable String runId,
            @RequestParam(defaultValue = "") EnumSet<Run.OptField> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId).findRunById(runId, removeNone(optFields));
    }

    /**
     *
     * [EXPERIMENTAL] Get runs by tag.
     *
     * <h2>Supported filter syntax</h2>
     *
     * <p>The filter string must contain one or more clauses. A clause is prefíxed
     * by a field name. Possible field names are:</p>
     *
     * <ul>
     *   <li><strong>tagName</strong> - Name of the tag</li>
     *   <li><strong>bool</strong>, <strong>integer</strong>, <strong>real</strong>, <strong>text</strong>, <strong>date</strong>, or <strong>time</strong> - tag value</li>
     * </ul>
     *
     * <p>The format of the <strong>date</strong> type is {@code yyyy-MM-dd} and of the <strong>time</strong> type is {@code HH\:mm\:ss}.</p>
     *
     * <p>A clause may be:</p>
     * <ul>
     *     <li>a <strong>term</strong>: field name followed by a colon and the search term, e.g. {@code tagName:my_name}</li>
     *     <li>a <strong>phrase</strong>: field name followed by a colon and the search phrase in doublequotes, e.g. {@code text:"new york"}</li>
     *     <li>a <strong>regular expression</strong>: field name followed by a colon and the regex in slashes, e.g. {@code text:/[mb]oat/}</li>
     *     <li>a <strong>comparison</strong>: field name followed by a comparison operator and a value, e.g. {@code integer<3}</li>
     *     <li>a <strong>range</strong>: field name followed by a colon and an open (indiced by {@code [ } and {@code ] }) or (semi-)closed range (indiced by <code>{</code> and <code>}</code>), e.g. {@code integer:[* TO 3] }</li>
     * </ul>
     *
     * <p>Clauses may be <strong>grouped</strong> with brackets {@code ( } and {@code ) } and / or <strong>joined</strong> with {@code AND} or {@code OR } (or {@code && } and {@code || })</p>
     *
     * <h3>Example</h3>
     *
     * <p>The syntax allows to build complex filter queries such as:</p>
     *
     * <p>{@code (tagName:hello || tagName:world) && text:"new york" AND text:/[mb]oat/ AND integer:[1 TO *] OR real<=3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date<2022-01-01 OR time:12\:00\:00 OR time:[12\:00\:00 TO 14\:00\:00] OR time<10\:00\:00 }</p>
     *
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId    project space to get runs from.
     * @param filter       tag filter.
     * @param pageable     pageable.
     * @param optFields    set of optional fields to be included. Use 'none' only to override defaults.
     * @return tagged runs
     */
    @Operation(operationId = "getRunsByTagExperimental")
    @Override
    public Page<Run> getObjectsByTag(String projectId, String filter, Pageable pageable, EnumSet<Run.OptField> optFields) {
        return TaggableController.super.getObjectsByTag(projectId, filter, pageable, optFields);
    }

    /**
     *
     * [EXPERIMENTAL] Add tags to a run in the project. Tags with the same name will be overwritten.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId  project-space to add to.
     * @param runId      run to add tags to.
     * @param tags       tags to add.
     * @return the tags that have been added
     */
    @Operation(operationId = "addTagsToRunExperimental")
    @PutMapping(value = "/tags/{runId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public List<? extends de.unijena.bioinf.ms.middleware.model.tags.Tag> addTags(@PathVariable String projectId, @PathVariable String runId, @Valid @RequestBody List<? extends de.unijena.bioinf.ms.middleware.model.tags.Tag> tags) {
        return TaggableController.super.addTags(projectId, runId, tags);
    }

    /**
     * [EXPERIMENTAL] Delete tag with the given name from the run with the specified ID in the specified project-space.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId     project-space to delete from.
     * @param runId         run to delete tag from.
     * @param tagName  name of the tag to delete.
     */
    @Operation(operationId = "removeTagFromRunExperimental")
    @DeleteMapping(value = "/tags/{runId}/{tagName}")
    @Override
    public void removeTags(String projectId, String runId, String tagName) {
        TaggableController.super.removeTags(projectId, runId, tagName);
    }

    /**
     * [EXPERIMENTAL] Get runs by tag group.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId project-space to delete from.
     * @param group     tag group name.
     * @param pageable  pageable.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return tagged runs
     */
    @Operation(operationId = "getRunsByGroupExperimental")
    @Override
    public Page<Run> getObjectsByGroup(String projectId, String group, Pageable pageable, EnumSet<Run.OptField> optFields) {
        return TaggableController.super.getObjectsByGroup(projectId, group, pageable, optFields);
    }

    @Override
    public Class<Run> getTagTarget() {
        return Run.class;
    }

}
