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
import de.unijena.bioinf.ms.middleware.model.tags.TagCategoryImport;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/categories")
@Tag(name = "Tag categories", description = "This API allows accessing tag categories.")
public class CategoryController {

    final protected ProjectsProvider<?> projectsProvider;

    @Autowired
    public CategoryController(ProjectsProvider<?> projectsProvider) {
        this.projectsProvider = projectsProvider;
    }

    /**
     * Get all tag categories in the given project-space.
     *
     * @param projectId project-space to read from.
     * @return Tag categories.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TagCategory> getCategories(@PathVariable String projectId) {
        return projectsProvider.getProjectOrThrow(projectId).findCategories();
    }

    /**
     * Get tag categories by type in the given project-space.
     *
     * @param projectId    project-space to read from.
     * @param categoryType name of the category
     * @return Tag categories.
     */
    @GetMapping(value = "/type/{categoryType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TagCategory> getCategoriesByType(@PathVariable String projectId, @PathVariable String categoryType) {
        return projectsProvider.getProjectOrThrow(projectId).findCategoriesByType(categoryType);
    }

    /**
     * Get tag category by name in the given project-space.
     *
     * @param projectId    project-space to read from.
     * @param categoryName name of the category
     * @return Tag category.
     */
    @GetMapping(value = "/{categoryName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TagCategory getCategoryByName(@PathVariable String projectId, @PathVariable String categoryName) {
        return projectsProvider.getProjectOrThrow(projectId).findCategoryByName(categoryName);
    }

    /**
     * Add tag categories to the project. Category names must not exist in the project.
     *
     * @param projectId  project-space to add to.
     * @param categories the tag categories to be added
     * @return the tag categories that have been added
     */
    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TagCategory> addCategories(
            @PathVariable String projectId,
            @Valid @RequestBody List<TagCategoryImport> categories
    ) {
        return projectsProvider.getProjectOrThrow(projectId).addCategories(categories, true);
    }

    /**
     * Add a possible value to the tag category in the project.
     *
     * @param projectId    project-space to add to.
     * @param categoryName the tag category to add to
     * @return the tag categories that have been added
     */
    @PutMapping(value = "/{categoryName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TagCategory addPossibleValuesToCategory(
            @PathVariable String projectId,
            @PathVariable String categoryName,
            @Valid @RequestBody List<?> possibleValues
    ) {
        return projectsProvider.getProjectOrThrow(projectId).addPossibleValuesToCategory(categoryName, possibleValues);
    }

    /**
     * Delete tag categories with the given names from the specified project-space.
     *
     * @param projectId     project-space to delete from.
     * @param categoryName  name of category to delete.
     */
    @DeleteMapping(value = "/{categoryName}")
    public void deleteCategories(@PathVariable String projectId, @PathVariable String categoryName) {
        projectsProvider.getProjectOrThrow(projectId).deleteCategory(categoryName);
    }

}
