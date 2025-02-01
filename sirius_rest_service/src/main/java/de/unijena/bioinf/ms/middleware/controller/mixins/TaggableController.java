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
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.List;

public interface TaggableController<T, O extends Enum<O>> extends ProjectProvidingController {
    Class<T> getTagTarget();
    //todo get by tag and get by group should be merged with usual getObject with generic lucene search query.
    /**
     * Get objects by tag.
     *
     * <h2>Supported filter syntax</h2>
     *
     * <p>The filter string must contain one or more clauses. A clause is prefíxed
     * by a field name. Possible field names are:</p>
     *
     * <ul>
     *   <li><strong>tagName</strong> - name of the tag</li>
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
     * @param projectId project space to get objects from.
     * @param filter    tag filter.
     * @param pageable  pageable.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return tagged objects
     */
    @GetMapping(value = "/tagged", produces = MediaType.APPLICATION_JSON_VALUE)
    default Page<T> getObjectsByTag(@PathVariable String projectId,
                                    @RequestParam(defaultValue = "") String filter,
                                    @ParameterObject Pageable pageable,
                                    @RequestParam(defaultValue = "") EnumSet<O> optFields
    ) {
        return getProjectsProvider().getProjectOrThrow(projectId).findObjectsByTagFilter(getTagTarget(), filter, pageable, optFields);
    }

    /**
     * Get group of objects by previously defined group.
     *
     * @param projectId project-space to delete from.
     * @param groupName     tag group name.
     * @param pageable  pageable.
     * @param optFields set of optional fields to be included. Use 'none' only to override defaults.
     * @return tagged objects
     */
    @GetMapping(value = "/grouped", produces = MediaType.APPLICATION_JSON_VALUE)
    default Page<T> getObjectsByGroup(@PathVariable String projectId,
                                      @RequestParam String groupName,
                                      @ParameterObject Pageable pageable,
                                      @RequestParam(defaultValue = "none") EnumSet<O> optFields
    ) {
        return getProjectsProvider().getProjectOrThrow(projectId).findObjectsByTagGroup(getTagTarget(), groupName, pageable, optFields);
    }

    /**
     * Get all tags associated with this Object
     *
     * @param projectId project-space to get from.
     * @param objectId  object to get tags for.
     * @return the tags of the requested object
     */
    @GetMapping(value = "/tags/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    default List<Tag> getTags(@PathVariable String projectId, @PathVariable String objectId) {
        return getProjectsProvider().getProjectOrThrow(projectId).findTagsByObject(getTagTarget(), objectId);
    }

    /**
     * Tags with the same name will be overwritten.
     *
     * @param projectId project-space to add to.
     * @param objectId  object to add tags to.
     * @param tags      tags to add.
     * @return the tags that have been added
     */
    @PutMapping(value = "/tags/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    default List<Tag> addTags(@PathVariable String projectId, @PathVariable String objectId, @Valid @RequestBody List<? extends Tag> tags) {
        return getProjectsProvider().getProjectOrThrow(projectId).addTagsToObject(getTagTarget(), objectId, tags);
    }


    /**
     * Remove tag with the given name from the object with the specified ID in the specified project-space.
     *
     * @param projectId    project-space to remove from.
     * @param objectId     object to remove tag from.
     * @param tagName name of the tag to remove.
     */
    @DeleteMapping(value = "/tags/{objectId}/{tagName}")
    default void removeTags(@PathVariable String projectId,
                            @PathVariable String objectId,
                            @PathVariable String tagName
    ) {
        getProjectsProvider().getProjectOrThrow(projectId).removeTagsFromObject(getTagTarget(), objectId, List.of(tagName));
    }
}
