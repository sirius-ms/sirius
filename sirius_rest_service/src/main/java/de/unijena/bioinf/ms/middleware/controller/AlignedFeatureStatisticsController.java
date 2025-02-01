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
import de.unijena.bioinf.ms.middleware.model.compute.Job;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChange;
import de.unijena.bioinf.ms.middleware.model.statistics.StatisticsTable;
import de.unijena.bioinf.ms.middleware.service.compute.ComputeService;
import de.unijena.bioinf.ms.middleware.service.projects.ProjectsProvider;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.List;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/aligned-features/statistics")
@Tag(name = "Feature Statistics", description =
        "[EXPERIMENTAL] This feature based API allows computing and accessing statistics for features (aligned over runs)." +
        "All endpoints are experimental and not part of the stable API specification. " +
        "These endpoints can change at any time, even in minor updates.")
public class AlignedFeatureStatisticsController implements StatisticsController<AlignedFeature> {

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

    @Operation(operationId = "computeAlignedFeatureFoldChangesExperimental")
    @Override
    public Job computeFoldChanges(String projectId, @NotNull String leftGroupName, @NotNull String rightGroupName, AggregationType aggregation, QuantMeasure quantification, EnumSet<Job.OptField> optFields) {
        return StatisticsController.super.computeFoldChanges(projectId, leftGroupName, rightGroupName, aggregation, quantification, optFields);
    }

    @Operation(operationId = "getAlignedFeatureFoldChangesExperimental")
    @Override
    public List<FoldChange> getFoldChanges(String projectId, @NotNull String leftGroupName, @NotNull String rightGroupName, AggregationType aggregation, QuantMeasure quantification) {
        return StatisticsController.super.getFoldChanges(projectId, leftGroupName, rightGroupName, aggregation, quantification);
    }

    @Operation(operationId = "deleteAlignedFeatureFoldChangesExperimental")
    @Override
    public void deleteFoldChanges(String projectId, @NotNull String leftGroupName, @NotNull String rightGroupName, AggregationType aggregation, QuantMeasure quantification) {
        StatisticsController.super.deleteFoldChanges(projectId, leftGroupName, rightGroupName, aggregation, quantification);
    }

    @Operation(operationId = "getAlignedFeatureFoldChangeTableExperimental")
    @Override
    public StatisticsTable getFoldChangeTable(String projectId, AggregationType aggregation, QuantMeasure quantification) {
        return StatisticsController.super.getFoldChangeTable(projectId, aggregation, quantification);
    }
}
