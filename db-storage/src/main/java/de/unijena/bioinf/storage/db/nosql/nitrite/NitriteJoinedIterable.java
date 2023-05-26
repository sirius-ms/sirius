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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

public class NitriteJoinedIterable<T, P> implements Iterable<T> {

    protected final Class<T> targetClass;

    protected org.dizitart.no2.Cursor parents;

    protected Function<Object, org.dizitart.no2.Cursor> children;

    protected final String localField;

    protected final String targetField;

    protected final NitriteMapper mapper;

    public NitriteJoinedIterable(
            Class<T> targetClass,
            Iterable<P> parents,
            Function<Object, org.dizitart.no2.Cursor> children,
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
        return new JoinedIterator(parents);
    }

    protected class JoinedIterator implements Iterator<T> {

        protected Iterator<Document> parentIterator;

        public JoinedIterator(org.dizitart.no2.Cursor parents) {
            this.parentIterator = parents.iterator();
        }

        @Override
        public boolean hasNext() {
            return parentIterator.hasNext();
        }

        @Override
        public T next() {
            Document targetDoc = new Document(parentIterator.next());
            Object localObject = targetDoc.get(localField);
            if (localObject == null) {
                return mapper.asObject(targetDoc, targetClass);
            }

            Set<Document> target = new HashSet<>();
            org.dizitart.no2.Cursor childCursor = children.apply(localObject);
            for (Document foreignDoc : childCursor) {
                target.add(foreignDoc);
            }
            if (!target.isEmpty()) {
                targetDoc.put(targetField, target);
            }

            return mapper.asObject(targetDoc, targetClass);
        }

    }

}
