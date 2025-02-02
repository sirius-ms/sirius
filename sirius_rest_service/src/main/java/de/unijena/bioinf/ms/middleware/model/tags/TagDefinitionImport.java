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

package de.unijena.bioinf.ms.middleware.model.tags;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagDefinitionImport {

    /**
     * Name of this tag defined by this definition (key)
     */
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected String tagName;

    /**
     * A human-readable description about the purpose of this tag.
     */
    @Nullable
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected String description;

    /**
     * A simple string based identifier to specify the type/scope/purpose of this tag.
     */
    @Nullable
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected String tagType;

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    protected ValueType valueType;


    @Nullable
    @Getter
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Builder.Default
    private List<Object> possibleValues = List.of();

    @Nullable
    @Getter
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Object minValue;

    @Nullable
    @Getter
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Object maxValue;
}
