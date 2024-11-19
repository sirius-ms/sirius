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

package de.unijena.bioinf.ms.middleware.controller.mixins;

import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.model.tags.TagGroup;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.EnumSet;
import java.util.List;

public interface TagController<T, O extends Enum<O>> extends ProjectProvidingController {

    SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    Class<T> getTagTarget();

    /**
     *
     * **EXPERIMENTAL** Get objects by tag.
     *
     * <h2>Supported filter syntax</h2>
     *
     * <p>The filter string must contain one or more clauses. A clause is prefíxed
     * by a field name. Possible field names are:</p>
     *
     * <ul>
     *   <li><strong>category</strong> - category name</li>
     *   <li><strong>bool</strong>, <strong>integer</strong>, <strong>real</strong>, <strong>text</strong>, <strong>date</strong>, or <strong>time</strong> - tag value</li>
     * </ul>
     *
     * <p>The format of the <strong>date</strong> type is {@code yyyy-MM-dd} and of the <strong>time</strong> type is {@code HH\:mm\:ss}.</p>
     *
     * <p>A clause may be:</p>
     * <ul>
     *     <li>a <strong>term</strong>: field name followed by a colon and the search term, e.g. {@code category:my_category}</li>
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
     * <p>{@code (category:hello || category:world) && text:"new york" AND text:/[mb]oat/ AND integer:[1 TO *] OR real<=3 OR date:2024-01-01 OR date:[2023-10-01 TO 2023-12-24] OR date<2022-01-01 OR time:12\:00\:00 OR time:[12\:00\:00 TO 14\:00\:00] OR time<10\:00\:00 }</p>
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId    project space to get objects from.
     * @param filter       tag filter.
     * @param pageable     pageable.
     * @param optFields    set of optional fields to be included. Use 'none' only to override defaults.
     * @return tagged objects
     */
    @GetMapping(value = "/tagged", produces = MediaType.APPLICATION_JSON_VALUE)
    default Page<T> objectsByTag(@PathVariable String projectId,
                                @RequestParam(defaultValue = "") String filter,
                                @ParameterObject Pageable pageable,
                                @RequestParam(defaultValue = "") EnumSet<O> optFields
    ) {
        return getProjectsProvider().getProjectOrThrow(projectId).findObjectsByTag(getTagTarget(), filter, pageable, optFields);
    }

    // TODO get tags for object? or add tags opt field to aligned features or compounds?

    /**
     *
     * Tags with the same category name will be overwritten.
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId  project-space to add to.
     * @param objectId   object to add tags to.
     * @param tags       tags to add.
     * @return the tags that have been added
     */
    @PutMapping(value = "/tags/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    default List<? extends Tag> addTags(@PathVariable String projectId, @PathVariable String objectId, @Valid @RequestBody List<? extends Tag> tags) {
        return getProjectsProvider().getProjectOrThrow(projectId).addTagsToObject(getTagTarget(), objectId, tags);
    }

    /**
     * **EXPERIMENTAL** Delete tag with the given category from the object with the specified ID in the specified project-space.
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId     project-space to delete from.
     * @param objectId      object to delete tag from.
     * @param categoryName  category name of the tag to delete.
     */
    @DeleteMapping(value = "/tags/{objectId}/{categoryName}")
    default void deleteTags(@PathVariable String projectId, @PathVariable String objectId, @PathVariable String categoryName) {
        getProjectsProvider().getProjectOrThrow(projectId).deleteTagsFromObject(objectId, List.of(categoryName));
    }

    /**
     * **EXPERIMENTAL** Get objects by tag group.
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId project-space to delete from.
     * @param group     tag group name.
     * @param pageable  pageable.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return tagged objects
     */
    @GetMapping(value = "/grouped", produces = MediaType.APPLICATION_JSON_VALUE)
    default Page<T> objectsByGroup(@PathVariable String projectId,
                                   @RequestParam String group,
                                   @ParameterObject Pageable pageable,
                                   @RequestParam(defaultValue = "") EnumSet<O> optFields
    ) {
        return getProjectsProvider().getProjectOrThrow(projectId).findObjectsByTagGroup(getTagTarget(), group, pageable, optFields);
    }

    /**
     * **EXPERIMENTAL** Get all tag category groups in the given project-space.
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId project-space to read from.
     * @return Tag category groups.
     */
    @GetMapping(value = "/groups", produces = MediaType.APPLICATION_JSON_VALUE)
    default List<TagGroup> getGroups(@PathVariable String projectId) {
        return getProjectsProvider().getProjectOrThrow(projectId).findTagGroups();
    }

    /**
     * **EXPERIMENTAL** Get tag groups by type in the given project-space.
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId project-space to read from.
     * @param groupType type of the group
     * @return Tag groups.
     */
    @GetMapping(value = "/groups/type/{groupType}", produces = MediaType.APPLICATION_JSON_VALUE)
    default List<TagGroup> getGroupsByType(@PathVariable String projectId, @PathVariable String groupType) {
        return getProjectsProvider().getProjectOrThrow(projectId).findTagGroupsByType(groupType);
    }

    /**
     * **EXPERIMENTAL** Get tag group by name in the given project-space.
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId project-space to read from.
     * @param groupName name of the group
     * @return Tag group.
     */
    @GetMapping(value = "/groups/{groupName}", produces = MediaType.APPLICATION_JSON_VALUE)
    default TagGroup getGroupByName(@PathVariable String projectId, @PathVariable String groupName) {
        return getProjectsProvider().getProjectOrThrow(projectId).findTagGroup(groupName);
    }

    /**
     *
     *
     * **EXPERIMENTAL** Group tags in the project. The group name must not exist in the project.
     *
     * <p>
     * See {@code /tagged} for filter syntax.
     * </p>
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId project-space to add to.
     * @param groupName name of the new group
     * @param filter    filter query to create the group
     * @param type      type of the group
     * @return the tag group that was added
     */
    @PutMapping(value = "/groups/{groupName}", produces = MediaType.APPLICATION_JSON_VALUE)
    default TagGroup addGroup(
            @PathVariable String projectId,
            @PathVariable String groupName,
            @RequestParam String filter,
            @RequestParam String type
    ) {
        return getProjectsProvider().getProjectOrThrow(projectId).addTagGroup(groupName, filter, type);
    }

    /**
     * **EXPERIMENTAL** Delete tag groups with the given name from the specified project-space.
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId  project-space to delete from.
     * @param groupName  name of group to delete.
     */
    @DeleteMapping(value = "/groups/{groupName}")
    default void deleteGroup(@PathVariable String projectId, @PathVariable String groupName) {
        getProjectsProvider().getProjectOrThrow(projectId).deleteTagGroup(groupName);
    }

}
