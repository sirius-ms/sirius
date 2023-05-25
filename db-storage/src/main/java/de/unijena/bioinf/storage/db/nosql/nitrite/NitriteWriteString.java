/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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

package de.unijena.bioinf.storage.db.nosql.nitrite;

import com.esotericsoftware.reflectasm.FieldAccess;

import java.lang.reflect.Field;
import java.util.Arrays;

public interface NitriteWriteString {

    default String writeString() {
        Class<? extends NitriteWriteString> clazz = getClass();
        StringBuilder tos = new StringBuilder(clazz.getSimpleName() + "{");
        FieldAccess access = FieldAccess.get(getClass());
        try {
            Field[] fields = access.getFields();
            Arrays.stream(fields).limit(fields.length - 1).forEach((Field field) -> {
                field.setAccessible(true);
                try {
                    tos.append(field.getName()).append("=").append(field.get(this)).append(",");
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
            tos.append(fields[fields.length - 1].getName()).append("=").append(fields[fields.length - 1].get(this));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        tos.append("}");
        return tos.toString();
    }

}
