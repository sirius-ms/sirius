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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value= Tag.class, name = "tag"),
        @JsonSubTypes.Type(value= Tag.BoolTag.class, name = "bool"),
        @JsonSubTypes.Type(value= Tag.IntTag.class, name = "int"),
        @JsonSubTypes.Type(value= Tag.DoubleTag.class, name = "double"),
        @JsonSubTypes.Type(value = Tag.StringTag.class, name = "string"),
        @JsonSubTypes.Type(value = Tag.DateTag.class, name = "date"),
        @JsonSubTypes.Type(value = Tag.TimeTag.class, name = "time")
})
public class Tag {

    /**
     * Name of the tag category
     */
    @NotNull
    private String category;

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BoolTag extends Tag {

        /**
         * Boolean value
         */
        @NotNull
        private Boolean bool;

    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IntTag extends Tag {

        /**
         * Integer value
         */
        @NotNull
        private Integer integer;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DoubleTag extends Tag {

        /**
         * Floating point value
         */
        private Double real;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StringTag extends Tag {

        /**
         * Text value
         */
        private String text;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DateTag extends Tag {

        /**
         * Date value in format yyyy-mm-dd
         */
        private String date;
    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TimeTag extends Tag {

        /**
         * Time value in format HH:mm:ss
         */
        private String time;
    }

}
