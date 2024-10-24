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

package de.unijena.bioinf.ms.persistence.model.core.tags;

import jakarta.persistence.Id;
import lombok.*;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class TagCategory {

    @Getter
    @AllArgsConstructor
    public enum ValueType {
        NONE(Void.class), BOOL(Boolean.class), INT(Integer.class), DOUBLE(Double.class), STRING(String.class), DATE(Long.class), TIME(Long.class);
        private final Class<?> valueClass;
    }

    @Id
    private long categoryId;

    private String name;

    private String description;

    private String type;

    private ValueType valueType;

    private List<?> possibleValues;

    private String categoryType;

    @Builder.Default
    private boolean editable = true;

}
