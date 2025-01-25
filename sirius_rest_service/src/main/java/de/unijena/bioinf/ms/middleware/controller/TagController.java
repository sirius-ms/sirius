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

import de.unijena.bioinf.ms.middleware.model.tags.TagDefinition;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinitionImport;
import de.unijena.bioinf.ms.middleware.model.tags.TagGroup;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/projects/{projectId}")
@Tag(name = "Tags and Groups", description = "[EXPERIMENTAL] This API allows managing tags and tag based data groupings. " +
        "All endpoints are experimental and not part of the stable API specification. " +
        "These endpoints can change at any time, even in minor updates.")
public class TagController {

    final protected ProjectsProvider<?> projectsProvider;

    @Autowired
    public TagController(ProjectsProvider<?> projectsProvider) {
        this.projectsProvider = projectsProvider;
    }

    /**
     * [EXPERIMENTAL] Get all tag definitions in the given project-space.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId project-space to read from.
     * @param tagScope scope of the tag (optional)
     * @return Tag definitions.
     */
    @GetMapping(value = "/tags", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TagDefinition> getTags(@PathVariable String projectId, @RequestParam(required = false) String tagScope) {
        if (tagScope == null || tagScope.isBlank())
            return projectsProvider.getProjectOrThrow(projectId).findTags();
        return projectsProvider.getProjectOrThrow(projectId).findTagsByScope(tagScope);
    }

    /**
     * [EXPERIMENTAL] Get tag definition by its name in the given project-space.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId    project-space to read from.
     * @param tagName name of the tag
     * @return Tag definition.
     */
    @GetMapping(value = "/tags/{tagName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TagDefinition getTag(@PathVariable String projectId, @PathVariable String tagName) {
        return projectsProvider.getProjectOrThrow(projectId).findTagByName(tagName);
    }

    /**
     * [EXPERIMENTAL] Add tags to the project. Tag names must not exist in the project.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId  project-space to add to.
     * @param tagDefinition the tag definitions to be created
     * @return the definitions of the tags that have been created
     */
    @PutMapping(value = "/tags", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TagDefinition> createTags(
            @PathVariable String projectId,
            @Valid @RequestBody List<TagDefinitionImport> tagDefinition
    ) {
        return projectsProvider.getProjectOrThrow(projectId).createTags(tagDefinition, true);
    }

    /**
     * [EXPERIMENTAL] Delete tag definition with the given name from the specified project-space.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId     project-space to delete from.
     * @param tagName  name of the tag definition to delete.
     */
    @DeleteMapping(value = "/tags/{tagName}")
    public void deleteTag(@PathVariable String projectId, @PathVariable String tagName) {
        projectsProvider.getProjectOrThrow(projectId).deleteTags(tagName);
    }

    /**
     * [EXPERIMENTAL] Add a possible value to the tag definition in the project.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId    project-space to add to.
     * @param tagName the tag definition to add the values to
     * @return the definitions of the tags that have been added
     */
    @PutMapping(value = "/tags/{tagName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TagDefinition addPossibleValuesToTagDefinition(
            @PathVariable String projectId,
            @PathVariable String tagName,
            @Valid @RequestBody List<?> possibleValues
    ) {
        return projectsProvider.getProjectOrThrow(projectId).addPossibleValuesToTagDefinition(tagName, possibleValues);
    }

    /**
     * [EXPERIMENTAL] Get all tag based groups in the given project-space.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId project-space to read from.
     * @param groupType type of the group
     * @return Groups.
     */
    @GetMapping(value = "/groups", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TagGroup> getGroups(@PathVariable String projectId, @RequestParam(required = false) String groupType) {
        if (groupType == null || groupType.isBlank())
            return projectsProvider.getProjectOrThrow(projectId).findTagGroups();
        return projectsProvider.getProjectOrThrow(projectId).findTagGroupsByType(groupType);
    }

    /**
     * [EXPERIMENTAL] Get tag groups by type in the given project-space.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId project-space to read from.
     * @param groupType type of the group
     * @return Tag groups.
     */
    @GetMapping(value = "/groups/type/{groupType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TagGroup> getGroupsByType(@PathVariable String projectId, @PathVariable String groupType) {
        return projectsProvider.getProjectOrThrow(projectId).findTagGroupsByType(groupType);
    }

    /**
     * [EXPERIMENTAL] Get tag group by name in the given project-space.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId project-space to read from.
     * @param groupName name of the group
     * @return Tag group.
     */
    @GetMapping(value = "/groups/{groupName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TagGroup getGroupByName(@PathVariable String projectId, @PathVariable String groupName) {
        return projectsProvider.getProjectOrThrow(projectId).findTagGroup(groupName);
    }

    /**
     * [EXPERIMENTAL] Group tags in the project. The group name must not exist in the project.
     *
     * <p>
     * See {@code /tagged} for filter syntax.
     * </p>
     *
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId project-space to add to.
     * @param groupName name of the new group
     * @param filter    filter query to create the group
     * @param type      type of the group
     * @return the tag group that was added
     */
    @PutMapping(value = "/groups/{groupName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TagGroup addGroup(
            @PathVariable String projectId,
            @PathVariable String groupName,
            @RequestParam String filter,
            @RequestParam String type
    ) {
        return projectsProvider.getProjectOrThrow(projectId).addTagGroup(groupName, filter, type);
    }

    /**
     * [EXPERIMENTAL] Delete tag groups with the given name from the specified project-space.
     * <p>
     * [EXPERIMENTAL] This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.
     *
     * @param projectId project-space to delete from.
     * @param groupName name of group to delete.
     */
    @DeleteMapping(value = "/groups/{groupName}")
    public void deleteGroup(@PathVariable String projectId, @PathVariable String groupName) {
        projectsProvider.getProjectOrThrow(projectId).deleteTagGroup(groupName);
    }

}
