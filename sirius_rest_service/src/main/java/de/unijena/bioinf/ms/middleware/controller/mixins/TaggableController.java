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
import de.unijena.bioinf.ms.middleware.model.tags.TagSubmission;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface TaggableController<T, O extends Enum<O>> extends ProjectProvidingController {
    Class<T> getTagTarget();

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
     * Tags with the same name will be overwritten.
     *
     * @param projectId project-space to add to.
     * @param tags      tags with id of the object to be added to.
     */
    @PutMapping(value = "/tags", produces = MediaType.APPLICATION_JSON_VALUE)
    default void addTagsToObjects(@PathVariable String projectId, @RequestBody @Valid List<TagSubmission> tags) {
        getProjectsProvider().getProjectOrThrow(projectId).addTagsToObjects(getTagTarget(), tags);
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
