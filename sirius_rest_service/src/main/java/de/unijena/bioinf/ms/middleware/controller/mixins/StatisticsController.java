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
import de.unijena.bioinf.ms.middleware.model.statistics.StatisticsTable;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
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
     * **EXPERIMENTAL** Compute the fold change between two groups of runs.
     *
     * The runs need to be tagged and grouped.
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId      project-space to compute the fold change in.
     * @param left           name of the left tag group.
     * @param right          name of the right tag group.
     * @param aggregation    aggregation type.
     * @param quantification quantification type.
     * @param optFields      job opt fields.
     * @return
     */
    @PutMapping(value = "/foldchange/compute",  produces = MediaType.APPLICATION_JSON_VALUE)
    default Job computeFoldChange(
            @PathVariable String projectId,
            @NotNull @RequestParam String left,
            @NotNull @RequestParam String right,
            @RequestParam(defaultValue = "AVG") AggregationType aggregation,
            @RequestParam(defaultValue = "APEX_INTENSITY") QuantMeasure quantification,
            @RequestParam(defaultValue = "progress") EnumSet<Job.OptField> optFields
            ) {
        return getComputeService().createAndSubmitFoldChangeJob(getProjectsProvider().getProjectOrThrow(projectId), left, right, aggregation, quantification, getTarget(), removeNone(optFields));
    }

    /**
     * **EXPERIMENTAL** Get table of all fold changes in the project space.
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId project-space to read from.
     * @param aggregation    aggregation type.
     * @param quantification quantification type.
     * @return table of fold changes.
     */
    @GetMapping(value = "/foldchange", produces = MediaType.APPLICATION_JSON_VALUE)
    default StatisticsTable getFoldChangeTable(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "AVG") AggregationType aggregation,
            @RequestParam(defaultValue = "APEX_INTENSITY") QuantMeasure quantification
    ) {
        return getProjectsProvider().getProjectOrThrow(projectId).getFoldChangeTable(getTarget(), aggregation, quantification);
    }

    /**
     * **EXPERIMENTAL** Page of all fold changes in the project space.
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId project-space to read from.
     * @param pageable  pageable.
     * @return fold changes.
     */
    @GetMapping(value = "/foldchange/page", produces = MediaType.APPLICATION_JSON_VALUE)
    default Page<F> listFoldChange(
            @PathVariable String projectId,
            @ParameterObject Pageable pageable
    ) {
        return getProjectsProvider().getProjectOrThrow(projectId).listFoldChanges(getTarget(), pageable);
    }

    /**
     * **EXPERIMENTAL** List all fold changes that are associated with an object.
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId project-space to read from.
     * @param objectId  id of the object the fold changes are assigned to.
     * @return fold changes
     */
    @GetMapping(value = "/foldchange/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    default List<F> getFoldChange(
            @PathVariable String projectId,
            @PathVariable String objectId
    ) {
        return getProjectsProvider().getProjectOrThrow(projectId).getFoldChanges(getTarget(), objectId);
    }

    /**
     * **EXPERIMENTAL** Delete fold change.
     *
     * <p>This endpoint is experimental and not part of the stable API specification. This endpoint can change at any time, even in minor updates.</p>
     *
     * @param projectId      project-space to delete from.
     * @param left           name of the left group.
     * @param right          name of the right group.
     * @param aggregation    aggregation type.
     * @param quantification quantification type.
     */
    @DeleteMapping(value = "/foldchange")
    default void deleteFoldChange(
            @PathVariable String projectId,
            @NotNull @RequestParam String left,
            @NotNull@RequestParam String right,
            @RequestParam(defaultValue = "AVG") AggregationType aggregation,
            @RequestParam(defaultValue = "APEX_INTENSITY") QuantMeasure quantification
    ) {
        getProjectsProvider().getProjectOrThrow(projectId).deleteFoldChange(getTarget(), left, right, aggregation, quantification);
    }

}
