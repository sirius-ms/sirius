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
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagCategory {

    public enum ValueRange {
        FIXED, VARIABLE
    }

    public enum ValueType {
        NONE, BOOLEAN, INTEGER, DOUBLE, STRING
    }

    @NotNull
    protected String name;

    @NotNull
    protected ValueType valueType;

    @NotNull
    @Builder.Default
    protected ValueRange valueRange = ValueRange.VARIABLE;

    @Nullable
    protected List<?> possibleValues;

    @Nullable
    protected String categoryType;

    public static TagCategoryBuilder builder() {
        return new TagCategoryBuilder() {
            @Override
            public TagCategory build() {
                TagCategory category = super.build();
                if (category.valueType == ValueType.NONE && category.possibleValues != null && !category.possibleValues.isEmpty()) {
                    throw new IllegalArgumentException("No possible values allowed.");
                }
                if (category.valueRange == ValueRange.VARIABLE && category.possibleValues != null && !category.possibleValues.isEmpty()) {
                    throw new IllegalArgumentException("No possible values allowed.");
                }
                if (category.valueRange == ValueRange.FIXED) {
                    if (category.possibleValues == null || category.possibleValues.isEmpty()) {
                        throw new IllegalArgumentException("No possible values provided.");
                    }
                    switch (category.valueType) {
                        case BOOLEAN -> {
                            for (Object o : category.possibleValues) {
                                if (!(o instanceof Boolean)) {
                                    throw new IllegalArgumentException(o + " is not a boolean.");
                                }
                            }
                        }
                        case INTEGER -> {
                            for (Object o : category.possibleValues) {
                                if (!(o instanceof Integer)) {
                                    throw new IllegalArgumentException(o + " is not an integer.");
                                }
                            }
                        }
                        case DOUBLE -> {
                            for (Object o : category.possibleValues) {
                                if (!(o instanceof Double)) {
                                    throw new IllegalArgumentException(o + " is not a double.");
                                }
                            }
                        }
                        case STRING -> {
                            for (Object o : category.possibleValues) {
                                if (!(o instanceof String)) {
                                    throw new IllegalArgumentException(o + " is not a String.");
                                }
                            }
                        }
                    }
                }
                return category;
            }
        };
    }

}
