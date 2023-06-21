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

import org.dizitart.no2.Document;
import org.dizitart.no2.mapper.NitriteMapper;
import org.dizitart.no2.objects.Cursor;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Function;

public class NitriteJoinedIterable<T, P> implements Iterable<T> {

    protected final Class<T> targetClass;

    protected final org.dizitart.no2.Cursor parents;

    protected final Function<Object, Iterable<Document>> children;

    protected final String localField;

    protected final String targetField;

    protected final NitriteMapper mapper;

    public NitriteJoinedIterable(
            Class<T> targetClass,
            Iterable<P> parents,
            Function<Object, Iterable<Document>> children,
            String localField,
            String targetField,
            NitriteMapper mapper
    ) throws IOException {
        if (!(parents instanceof Cursor)) {
            throw new IOException("parents must be a cursor!");
        }
        try {
            Field cField = parents.getClass().getDeclaredField("cursor");
            cField.setAccessible(true);
            this.parents = (org.dizitart.no2.Cursor) cField.get(parents);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IOException(e);
        }
        this.children = children;
        this.targetClass = targetClass;
        this.localField = localField;
        this.targetField = targetField;
        this.mapper = mapper;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        if (parents.size() == 0) {
            return Collections.emptyIterator();
        }
        return new CombiningIterator<>(targetClass, new JoinedDocumentIterator(parents, children, localField, targetField), mapper);
    }
}
