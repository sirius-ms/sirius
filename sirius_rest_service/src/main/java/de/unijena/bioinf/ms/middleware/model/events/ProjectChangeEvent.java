/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.middleware.model.events;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Event informs about changes in the specific project define by the 'projectId'.
 * The event contains multiple hierarchical id fields which define on which level the change has
 * happened or how specific the event is.
 *
 * e.g. if `formulaId` is not null then you can be sure that only data of this formula id
 * or lower (more specific) has been changed
 */
@Getter
@Setter
@Builder
public class ProjectChangeEvent {
    public enum Type {
        PROJECT_OPENED,
        PROJECT_MOVED,
        PROJECT_CLOSED,

        FEATURE_CREATED,
        FEATURE_UPDATED,
        FEATURE_DELETED,

        RESULT_CREATED,
        RESULT_UPDATED,
        RESULT_DELETED
    }

    /**
     * Type of change that has happened.
     */
    @Schema
    private Type eventType;

    /**
     * Project on which the change has happened (is allways given).
     * If no other `id` is given this indicates that the whole project has been changed
     * (e.g. PROJECT_OPENED, PROJECT_MOVED, PROJECT_CLOSED).
     */
    @Schema
    private String projectId;

    /**
     * Compound on which the change has happened (optional).
     * Usually given together with featureId since changes in a compound are usually
     * caused by changes on the feature level
     */
    @Schema(nullable = true)
    protected String compoundId;

    /**
     * Feature (aligned over runs) on which the change has happened (optional).
     */
    @Schema(nullable = true)
    protected String featuredId;

    /**
     * Formula candidate on which the change has happened (optional).
     */
    @Schema(nullable = true)
    protected String formulaId;

    /**
     * Structure candidate (identified by its 2d InChI key) on which the change has happened (optional).
     */
    @Schema(nullable = true)
    protected String structureInChIKey;
}
