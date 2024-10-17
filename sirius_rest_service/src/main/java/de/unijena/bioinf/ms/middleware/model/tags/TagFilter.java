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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TagFilter.TaggedFilter.class, name = "tagged"),
        @JsonSubTypes.Type(value = TagFilter.BoolTagFilter.class, name = "bool"),
        @JsonSubTypes.Type(value = TagFilter.IntTagFilter.class, name = "int"),
        @JsonSubTypes.Type(value = TagFilter.DoubleTagFilter.class, name = "double"),
        @JsonSubTypes.Type(value = TagFilter.StringTagFilter.class, name = "string")
})
public abstract class TagFilter {

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BoolTagFilter extends TagFilter {

        @Builder.Default
        protected boolean value = true;

    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract static class ComparableTagFilter<T extends Comparable<T>> extends TagFilter {

        @Nullable
        protected T equals;

        @Nullable
        protected T lessThan;

        @Nullable
        protected T lessThanEquals;

        @Nullable
        protected T greaterThan;

        @Nullable
        protected T greaterThanEquals;

    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DoubleTagFilter extends ComparableTagFilter<Double> {

    }


    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IntTagFilter extends ComparableTagFilter<Integer> {

    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StringTagFilter extends TagFilter {

        @Nullable
        protected String equals;

        @Nullable
        protected String notEquals;

        @Nullable
        protected String contains;

        @Nullable
        protected String regexMatch;

    }

    @Getter
    @Setter
    @SuperBuilder
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TaggedFilter extends TagFilter {

        @Builder.Default
        protected boolean value = true;

    }

}
