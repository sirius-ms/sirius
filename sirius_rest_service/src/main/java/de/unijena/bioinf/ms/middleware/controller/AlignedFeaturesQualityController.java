/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.ms.middleware.model.features.AlignedFeatureQuality;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/aligned-features-quality")
@Tag(name = "Data Quality API", description = "Access data quality information for various entities of a specified project-space.")
@ConditionalOnProperty("de.unijena.bioinf.ms.middleware.controller.quality.enabled")
public class AlignedFeaturesQualityController {

    private final ProjectsProvider<?> projectsProvider;

    @Autowired
    public AlignedFeaturesQualityController(ProjectsProvider<?> projectsProvider) {
        this.projectsProvider = projectsProvider;
    }


    /**
     * Get data quality information for features (aligned over runs) in the given project-space.
     *
     * @param projectId project-space to read from.
     * @param optFields set of optional fields to be included
     * @return AlignedFeatureQuality quality information of the respective feature.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<AlignedFeatureQuality> getAlignedFeaturesQuality(
            @PathVariable String projectId,
            @ParameterObject Pageable pageable,
            @RequestParam(defaultValue = "qualityFlags, lcmsFeatureQuality") EnumSet<AlignedFeatureQuality.OptFields> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId).findAlignedFeaturesQuality(pageable, optFields);
    }


    /**
     * Get data quality information for feature (aligned over runs) with the given identifier from the specified project-space.
     *
     * @param projectId      project-space to read from.
     * @param alignedFeatureId identifier of feature (aligned over runs) to access.
     * @param optFields      set of optional fields to be included
     * @return AlignedFeatureQuality quality information of the respective feature.
     */
    @GetMapping(value = "/{alignedFeatureId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AlignedFeatureQuality getAlignedFeaturesQuality(
            @PathVariable String projectId, @PathVariable String alignedFeatureId,
            @RequestParam(defaultValue = "qualityFlags, lcmsFeatureQuality") EnumSet<AlignedFeatureQuality.OptFields> optFields
    ) {
        return projectsProvider.getProjectOrThrow(projectId).findAlignedFeaturesQualityById(alignedFeatureId, optFields);
    }
}

