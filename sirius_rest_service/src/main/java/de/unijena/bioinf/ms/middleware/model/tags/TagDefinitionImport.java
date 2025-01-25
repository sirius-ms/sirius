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
import de.unijena.bioinf.ms.middleware.controller.mixins.TaggableController;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagDefinitionImport {

    public enum ValueType {
        NONE, BOOLEAN, INTEGER, DOUBLE, STRING, DATE, TIME
    }

    /**
     * Name of this tag defined by this definition (key)
     */
    @NotNull
    protected String tagName;

    @Nullable
    protected String description;

    @NotNull
    protected ValueType valueType;

    @Nullable
    protected List<?> possibleValues;

    /**
     * A simple string based identifier to specify the type/scope/purpose of this tag.
     */
    @Nullable
    protected String tagType;

    public abstract static class TagDefinitionImportBuilder<C extends TagDefinitionImport, B extends TagDefinitionImportBuilder<C, B>> {

        private boolean possibleValuesChecked = false;

        public B valueType(ValueType valueType) {
            if (!possibleValuesChecked) {
                throw new UnsupportedOperationException("Please use 'valueTypeAndPossibleValues' instead!");
            }
            this.valueType = valueType;
            return self();
        }

        public B possibleValues(List<?> possibleValues) {
            if (!possibleValuesChecked) {
                throw new UnsupportedOperationException("Please use 'valueTypeAndPossibleValues' instead!");
            }
            this.possibleValues = possibleValues;
            return self();
        }

        public B valueTypeAndPossibleValues(@NotNull ValueType valueType, @Nullable List<?> possibleValues) {
            if (possibleValues != null && !possibleValues.isEmpty()) {
                switch (valueType) {
                    case NONE: throw new IllegalArgumentException("No possible values allowed.");
                    case BOOLEAN:
                        for (Object o : possibleValues) {
                            if (!(o instanceof Boolean)) {
                                throw new IllegalArgumentException(o + " is not a boolean");
                            }
                        }
                        break;
                    case INTEGER:
                        for (Object o : possibleValues) {
                            if (!(o instanceof Integer)) {
                                throw new IllegalArgumentException(o + " is not an integer");
                            }
                        }
                        break;
                    case DOUBLE:
                        for (Object o : possibleValues) {
                            if (!(o instanceof Double)) {
                                throw new IllegalArgumentException(o + " is not a double");
                            }
                        }
                        break;
                    case STRING:
                        for (Object o : possibleValues) {
                            if (!(o instanceof String)) {
                                throw new IllegalArgumentException(o + " is not a String");
                            }
                        }
                        break;
                    case DATE:
                        for (Object o : possibleValues) {
                            if (o instanceof String s) {
                                try {
                                    TaggableController.DATE_FORMAT.parse(s);
                                } catch (ParseException e) {
                                    throw new IllegalArgumentException(o + " is not a date in format yyyy-MM-dd");
                                }
                            } else {
                                throw new IllegalArgumentException(o + " is not a String");
                            }
                        }
                        break;
                    case TIME:
                        for (Object o : possibleValues) {
                            if (o instanceof String s) {
                                try {
                                    TaggableController.TIME_FORMAT.parse(s);
                                } catch (ParseException e) {
                                    throw new IllegalArgumentException(o + " is not a date in format yyyy-MM-dd");
                                }
                            } else {
                                throw new IllegalArgumentException(o + " is not a String");
                            }
                        }
                        break;
                }
            }
            possibleValuesChecked = true;
            return valueType(valueType).possibleValues(possibleValues);
        }

    }

}
