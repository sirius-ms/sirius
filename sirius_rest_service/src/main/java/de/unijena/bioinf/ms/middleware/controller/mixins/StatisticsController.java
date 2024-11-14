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

import de.unijena.bioinf.ms.middleware.model.compute.Job;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChange;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantificationType;
import org.jetbrains.annotations.NotNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.List;

import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.removeNone;

public interface StatisticsController<T, F extends FoldChange> extends ProjectProvidingController, ComputeServiceController {


    Class<T> getTarget();

    /**
     * TODO doc
     * Import and Align full MS-Runs from various formats into the specified project as background job.
     * Possible formats (mzML, mzXML)
     *
     * @param projectId    Project-space to import into.
     * @return the import job.
     */
    @PutMapping(value = "/foldchange/compute",  produces = MediaType.APPLICATION_JSON_VALUE)
    default Job computeFoldChange(
            @PathVariable String projectId,
            @NotNull @RequestParam String left,
            @NotNull @RequestParam String right,
            @RequestParam(defaultValue = "AVG") AggregationType aggregation,
            @RequestParam(defaultValue = "APEX_INTENSITY") QuantificationType quantification,
            @RequestParam(defaultValue = "progress") EnumSet<Job.OptField> optFields
            ) {
        // TODO test this
        return getComputeService().createAndSubmitFoldChangeJob(getProjectsProvider().getProjectOrThrow(projectId), left, right, aggregation, quantification, getTarget(), removeNone(optFields));
    }

    @GetMapping(value = "/foldchange", produces = MediaType.APPLICATION_JSON_VALUE)
    default Page<F> listFoldChange(
            @PathVariable String projectId,
            @ParameterObject Pageable pageable
    ) {
        return getProjectsProvider().getProjectOrThrow(projectId).listFoldChanges(getTarget(), pageable);
    }

    @GetMapping(value = "/foldchange/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    default List<F> getFoldChange(
            @PathVariable String projectId,
            @PathVariable String objectId,
            @ParameterObject Pageable pageable
    ) {
        return getProjectsProvider().getProjectOrThrow(projectId).getFoldChanges(getTarget(), objectId);
    }

    @DeleteMapping(value = "/foldchange")
    default void deleteFoldChange(
            @PathVariable String projectId,
            @NotNull @RequestParam String left,
            @NotNull@RequestParam String right,
            @RequestParam(defaultValue = "AVG") AggregationType aggregation,
            @RequestParam(defaultValue = "APEX_INTENSITY") QuantificationType quantification
    ) {
        getProjectsProvider().getProjectOrThrow(projectId).deleteFoldChange(getTarget(), left, right, aggregation, quantification);
    }

}
