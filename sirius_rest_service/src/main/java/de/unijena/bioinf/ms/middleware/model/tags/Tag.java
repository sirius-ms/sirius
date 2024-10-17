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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value= Tag.BoolTag.class, name = "bool"),
        @JsonSubTypes.Type(value= Tag.IntTag.class, name = "int"),
        @JsonSubTypes.Type(value= Tag.DoubleTag.class, name = "double"),
        @JsonSubTypes.Type(value = Tag.StringTag.class, name = "string")
})
public class Tag {

    /**
     * Name of the tag category
     */
    @NotNull
    private String categoryName;

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract static class ValueTag<T> extends Tag {

        /**
         * Tag value
         */
        @Nullable
        private T value;

    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BoolTag extends ValueTag<Boolean> {

    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IntTag extends ValueTag<Integer> {

    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DoubleTag extends ValueTag<Double> {

    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StringTag extends ValueTag<String> {

    }

}
