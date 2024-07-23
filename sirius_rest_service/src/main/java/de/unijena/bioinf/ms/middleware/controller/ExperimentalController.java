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

import de.unijena.bioinf.ms.middleware.model.features.AlignedFeatureQuality;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "EXPERIMENTAL", description = "All Endpoints are experimental and not part of the stable API specification. " +
        "This endpoints can change at any time, even in minor updates.")
public class ExperimentalController {

    private final ProjectsProvider<?> projectsProvider;

    @Autowired
    public ExperimentalController(ProjectsProvider<?> projectsProvider) {
        this.projectsProvider = projectsProvider;
    }

    /**
     * Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.
     *
     * EXPERIMENTAL: Endpoint is not part of the stable API specification and might change in minor updates.
     *
     * @param projectId      project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @return AlignedFeatureQuality quality information of the respective feature.
     */
    @GetMapping(value = "/api/projects/{projectId}/aligned-features/{alignedFeatureId}/quality-report", produces = MediaType.APPLICATION_JSON_VALUE)
    public AlignedFeatureQuality getAlignedFeaturesQuality(
            @PathVariable String projectId, @PathVariable String alignedFeatureId
    ) {
        return projectsProvider.getProjectOrThrow(projectId).findAlignedFeaturesQualityById(alignedFeatureId);
    }

}
