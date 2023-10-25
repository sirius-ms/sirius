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

import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/compounds")
@Tag(name = "Compound based API", description = "This allows to retrieve all AlignedFeatures that belong to the same "
        + "compound (also known as a group of ion identities). It also provides for each AlignedFeature the "
        + "corresponding annotation results (which are usually computed on a per-feature basis)")
public class CompoundController {


    private final ProjectsProvider<?> projectsProvider;

    @Autowired
    public CompoundController(ProjectsProvider<?> projectsProvider) {
        this.projectsProvider = projectsProvider;
    }

    /**
     * Get all available compounds (group of ion identities) in the given project-space.
     *
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included
     * @return Compounds with additional optional fields (if specified).
     */

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<Compound> getCompounds(@PathVariable String projectId, @ParameterObject Pageable pageable,
                                       @RequestParam(required = false, defaultValue = "") EnumSet<Compound.OptFields> optFields) {
        return projectsProvider.getProjectOrThrow(projectId).findCompounds(pageable, optFields);
    }


    /**
     * Get compound (group of ion identities) with the given identifier from the specified project-space.
     *
     * @param projectId  project-space to read from.
     * @param compoundId identifier of the compound (group of ion identities) to access.
     * @param optFields  set of optional fields to be included
     * @return Compounds with additional optional fields (if specified).
     */
    @GetMapping(value = "/{compoundId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Compound getCompound(@PathVariable String projectId, @PathVariable String compoundId,
                                @RequestParam(required = false, defaultValue = "") EnumSet<Compound.OptFields> optFields) {
        return projectsProvider.getProjectOrThrow(projectId).findCompoundById(compoundId, optFields);
    }


    /**
     * Delete compound (group of ion identities) with the given identifier (and the included features) from the
     * specified project-space.
     *
     * @param projectId  project-space to delete from.
     * @param compoundId identifier of the compound to delete.
     */
    @DeleteMapping(value = "/{compoundId}")
    public void deleteAlignedFeature(@PathVariable String projectId, @PathVariable String compoundId) {
        projectsProvider.getProjectOrThrow(projectId).deleteCompoundById(compoundId);
    }
}
