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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

public interface StatisticsController<F extends FoldChange> extends ProjectProvidingController, ComputeServiceController {

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
            @PathVariable String leftGroupName,
            @PathVariable String rightGroupName,
            @PathVariable AggregationType aggregationType,
            @PathVariable QuantificationType quantificationType
            ) {
        // TODO
        return null;
    }

    @GetMapping(value = "/foldchange", produces = MediaType.APPLICATION_JSON_VALUE)
    default Page<F> listFoldChange(
            @PathVariable String projectId,
            @PathVariable String leftGroupName,
            @PathVariable String rightGroupName,
            @ParameterObject Pageable pageable
    ) {
        return null;
//        return projectsProvider.getProjectOrThrow(projectId).findObjectsByTag(getTaggable(), filter, pageable, optFields);
    }

    @GetMapping(value = "/foldchange/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    default F getFoldChange(
            @PathVariable String projectId,
            @PathVariable String objectId,
            @PathVariable String leftGroupName,
            @PathVariable String rightGroupName
    ) {
        return null;
    }

    @DeleteMapping(value = "/foldchange")
    default void deleteFoldChange(
            @PathVariable String projectId,
            @PathVariable String leftGroupName,
            @PathVariable String rightGroupName
    ) {
//        return projectsProvider.getProjectOrThrow(projectId).findObjectsByTag(getTaggable(), filter, pageable, optFields);
    }

}
