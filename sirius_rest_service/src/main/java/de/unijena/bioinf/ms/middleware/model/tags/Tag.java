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
import de.unijena.bioinf.ms.middleware.controller.mixins.TaggableController;
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
     * Type of the value of the tag as defined in the corresponding tag definition.
     */
    @NotNull
    private TagDefinitionImport.ValueType valueType;

    @Nullable
    private Boolean bool;

    @Nullable
    private Integer integer;

    @Nullable
    private Double real;

    @Nullable
    private String text;

    @Nullable
    private String date;

    @Nullable
    private String time;

    public static TagBuilder builder() {
        return new TagBuilder() {

            @Override
            public TagBuilder date(@Nullable String date) {
                try {
                    TaggableController.DATE_FORMAT.parse(date);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Illegal date value: " + date);
                }
                return super.date(date);
            }

            public TagBuilder time(@Nullable String time) {
                try {
                    TaggableController.TIME_FORMAT.parse(time);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Illegal time value: " + time);
                }
                return super.time(time);
            }

            @SneakyThrows
            public Tag build() {
                Tag tag = super.build();

                int fieldSet = 0;
                if (tag.getBool() != null) fieldSet |= 0x000001;
                if (tag.getInteger() != null) fieldSet |= 0x000010;
                if (tag.getReal() != null) fieldSet |= 0x000100;
                if (tag.getText() != null) fieldSet |= 0x001000;
                if (tag.getDate() != null) fieldSet |= 0x010000;
                if (tag.getTime() != null) fieldSet |= 0x100000;

                switch (tag.getValueType()) {
                    case NONE: if (fieldSet != 0) throw new IllegalArgumentException("No values allowed."); break;
                    case BOOLEAN: if (fieldSet != 0x000001) throw new IllegalArgumentException("Only boolean values allowed."); break;
                    case INTEGER: if (fieldSet != 0x000010) throw new IllegalArgumentException("Only integer values allowed."); break;
                    case DOUBLE: if (fieldSet != 0x000100) throw new IllegalArgumentException("Only floating point values allowed."); break;
                    case STRING: if (fieldSet != 0x001000) throw new IllegalArgumentException("Only string values allowed."); break;
                    case DATE: if (fieldSet != 0x010000) throw new IllegalArgumentException("Only date values allowed."); break;
                    case TIME: if (fieldSet != 0x100000) throw new IllegalArgumentException("Only time values allowed."); break;
                }

                return tag;
            }
        };

    }

}
