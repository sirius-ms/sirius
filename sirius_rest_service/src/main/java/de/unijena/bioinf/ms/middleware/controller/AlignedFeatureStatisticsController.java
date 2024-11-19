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

import de.unijena.bioinf.ms.middleware.controller.mixins.StatisticsController;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChange;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/aligned-features/statistics")
@Tag(name = "Feature Statistics", description =
        "**EXPERIMENTAL** This feature based API allows computing and accessing statistics for features (aligned over runs)." +
        "All endpoints are experimental and not part of the stable API specification. " +
        "These endpoints can change at any time, even in minor updates.")
public class AlignedFeatureStatisticsController implements StatisticsController<AlignedFeature, FoldChange.AlignedFeatureFoldChange> {

    @Getter
    private final ComputeService computeService;

    @Getter
    private final ProjectsProvider<?> projectsProvider;

    @Autowired
    public AlignedFeatureStatisticsController(ComputeService computeService, ProjectsProvider<?> projectsProvider) {
        this.computeService = computeService;
        this.projectsProvider = projectsProvider;
    }

    @Override
    public Class<AlignedFeature> getTarget() {
        return AlignedFeature.class;
    }

    /**
     * **EXPERIMENTAL** List all fold changes that are associated with a feature (aligned over runs).
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId        project-space to read from.
     * @param alignedFeatureId id of the feature (aligend over runs) the fold changes are assigned to.
     * @return fold changes
     */
    @GetMapping(value = "/foldchange/{alignedFeatureId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public List<FoldChange.AlignedFeatureFoldChange> getFoldChange(String projectId, String alignedFeatureId) {
        return StatisticsController.super.getFoldChange(projectId, alignedFeatureId);
    }

}
