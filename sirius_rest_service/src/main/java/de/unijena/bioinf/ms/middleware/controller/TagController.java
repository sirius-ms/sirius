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

import de.unijena.bioinf.ms.middleware.model.tags.TagCategory;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public abstract class TagController {

    final protected ProjectsProvider<?> projectsProvider;

    protected TagController(ProjectsProvider<?> projectsProvider) {
        this.projectsProvider = projectsProvider;
    }

    protected abstract Project.Taggable getTaggable();

    /**
     * Add tags to a run in the project. Tags with the same category name will be overwritten.
     *
     * @param projectId  project-space to add to.
     * @param objectId   object to tag.
     * @param tags       tags to add.
     * @return the tags that have been added
     */
    @PostMapping(value = "/tags/add/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Tag> addTags(@PathVariable String projectId, @PathVariable String objectId, @Valid @RequestBody List<Tag> tags) {
        return projectsProvider.getProjectOrThrow(projectId).addTagsToObject(getTaggable(), objectId, tags);
    }

    /**
     * Delete tags with the given IDs from the specified project-space.
     *
     * @param projectId     project-space to delete from.
     * @param objectId      object to delete tags from.
     * @param categoryNames Category names of the tags to delete.
     */
    @PutMapping(value = "/tags/delete/{objectId}")
    public void deleteTags(@PathVariable String projectId, @PathVariable String objectId, @RequestBody List<String> categoryNames) {
        projectsProvider.getProjectOrThrow(projectId).deleteTagsFromObject(getTaggable(), objectId, categoryNames);
    }

    /**
     * Get all tag categories in the given project-space.
     *
     * @param projectId project-space to read from.
     * @return Tag categories.
     */
    @GetMapping(value = "/categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TagCategory> getCategories(@PathVariable String projectId) {
        return projectsProvider.getProjectOrThrow(projectId).findCategories(getTaggable());
    }

    /**
     * Get tag category by name in the given project-space.
     *
     * @param projectId    project-space to read from.
     * @param categoryName name of the category
     * @return Tag categories.
     */
    @GetMapping(value = "/categories/{categoryName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TagCategory getCategoryByName(@PathVariable String projectId, @PathVariable String categoryName) {
        return projectsProvider.getProjectOrThrow(projectId).findCategoryByName(getTaggable(), categoryName);
    }

    /**
     * Add tag category to the project. Category name must not exist in the project.
     *
     * @param projectId  project-space to add to.
     * @param categories the tag categories to be added
     * @return the tag categories that have been added
     */
    @PostMapping(value = "/categories/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TagCategory> addCategories(@PathVariable String projectId, @Valid @RequestBody List<TagCategory> categories) {
        return projectsProvider.getProjectOrThrow(projectId).addCategories(getTaggable(), categories);
    }

    /**
     * Delete tag categories with the given names from the specified project-space.
     *
     * @param projectId     project-space to delete from.
     * @param categoryNames names of categories to delete.
     */
    @PutMapping(value = "/categories/delete")
    public void deleteCategories(@PathVariable String projectId, @RequestBody List<String> categoryNames) {
        projectsProvider.getProjectOrThrow(projectId).deleteCategories(getTaggable(), categoryNames);
    }

}
