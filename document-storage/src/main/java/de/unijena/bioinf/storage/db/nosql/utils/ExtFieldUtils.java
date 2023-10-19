/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.storage.db.nosql.utils;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.Objects;

public class ExtFieldUtils extends FieldUtils {
    public static <T> Field getAllField(Class<T> clz, String name) {
        Field field = null;
        for (Field tmpfield : FieldUtils.getAllFields(clz)) {
            if (tmpfield.getName().equals(name)) {
                field = tmpfield;
                break;
            }
        }
        Objects.requireNonNull(field, "Field with name '" + name + "' could not be found in class '" + clz.getName() + '.');
        field.setAccessible(true);

        return field;
    }

    public static <T> Object getAllFieldValue(T object, String name) throws IllegalAccessException {
        Field field = getAllField(object.getClass(), name);
        return field.get(object);
    }
}
