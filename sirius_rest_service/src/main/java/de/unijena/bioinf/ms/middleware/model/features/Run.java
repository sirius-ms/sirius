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

package de.unijena.bioinf.ms.middleware.model.features;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Run {

    @Schema(enumAsRef = true, name = "RunOptField", nullable = true)
    public enum OptField {none, tags}

    /**
     * Identifier
     */
    @NotNull
    protected String runId;

    /**
     * Informative, human-readable name of this run
     */
    protected String name;

    @Schema(nullable = true)
    protected String chromatography;

    @Schema(nullable = true)
    protected String ionization;

    @Schema(nullable = true)
    protected String fragmentation;

    @Schema(nullable = true)
    protected List<String> massAnalyzers;

    /**
     * Key: tagName, value: tag
     */
    @Schema(nullable = true)
    protected Map<String, ? extends Tag> tags;

}
