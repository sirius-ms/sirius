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
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
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
@RequestMapping(value = "/api/projects/{projectId}/compounds/statistics")
@Tag(name = "Compound Statistics", description = "**EXPERIMENTAL** This compound based API allows allows computing and accessing statistics for compounds (also known as a group of ion identities). " +
        "All endpoints are experimental and not part of the stable API specification. " +
        "These endpoints can change at any time, even in minor updates.")
public class CompoundStatisticsController implements StatisticsController<Compound, FoldChange.CompoundFoldChange> {

    @Getter
    private final ComputeService computeService;

    @Getter
    private final ProjectsProvider<?> projectsProvider;

    @Autowired
    public CompoundStatisticsController(ComputeService computeService, ProjectsProvider<?> projectsProvider) {
        this.computeService = computeService;
        this.projectsProvider = projectsProvider;
    }

    @Override
    public Class<Compound> getTarget() {
        return Compound.class;
    }

    /**
     * **EXPERIMENTAL** List all fold changes that are associated with a compound (group of ion identities).
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId  project-space to read from.
     * @param compoundId id of the compound (group of ion identities) the fold changes are assigned to.
     * @return fold changes
     */
    @GetMapping(value = "/foldchange/{compoundId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public List<FoldChange.CompoundFoldChange> getFoldChange(String projectId, String compoundId) {
        return StatisticsController.super.getFoldChange(projectId, compoundId);
    }

}
