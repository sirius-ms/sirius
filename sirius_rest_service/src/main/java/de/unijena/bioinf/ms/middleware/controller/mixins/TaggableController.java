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
    Page<T> getObjectsByGroup(@PathVariable String projectId,
                                      @RequestParam String groupName,
                                      @ParameterObject Pageable pageable,
                                      @RequestParam(defaultValue = "none") EnumSet<O> optFields
    );
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
