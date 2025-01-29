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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag {

    /**
     * Name of the tag as defined by the corresponding TagDefinition
     * Links tag object to their definition.
     */
    @NotNull
    private String tagName;

    /**
     * Optional value of the tag.
     * <p>
     * Generic value of the tag as defined by the corresponding TagDefinition.
     * Can be Integer, Double, Boolean and String, whereas String values can represent Text, Date (yyyy-MM-dd) or Time (HH:mm:ss).
     */
    @Schema(oneOf = { Integer.class, Double.class, Boolean.class, String.class},
            nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Nullable
    private Object value;




}
