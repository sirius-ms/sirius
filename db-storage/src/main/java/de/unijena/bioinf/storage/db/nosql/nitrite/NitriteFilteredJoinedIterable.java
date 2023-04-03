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

import de.unijena.bioinf.storage.db.nosql.Filter;
import org.dizitart.no2.Document;
import org.dizitart.no2.Lookup;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.RecordIterable;
import org.dizitart.no2.objects.Cursor;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.dizitart.no2.Document.createDocument;

public class NitriteFilteredJoinedIterable<T, P, C> implements Iterable<T> {

    private final Class<T> clazz;
    private final Class<P> parentClass;
    private final Class<C> childClass;
    private final Filter childFilter;
    private final Set<Long> parents;
    private final String foreignField;
    private final String targetField;

    private final NitriteDatabase database;

    public NitriteFilteredJoinedIterable(Class<T> clazz, Class<P> parentClass, Class<C> childClass, Filter childFilter, Cursor<P> parents, String foreignField, String targetField, NitriteDatabase database) {
        this.clazz = clazz;
        this.parentClass = parentClass;
        this.childClass = childClass;
        this.childFilter = childFilter;
        this.parents = parents.idSet().stream().mapToLong(NitriteId::getIdValue).boxed().collect(Collectors.toSet());
        this.foreignField = foreignField;
        this.targetField = targetField;
        this.database = database;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        if (parents.isEmpty()) {
            return Collections.emptyIterator();
        }
        return new FilteredJoinedIterator(clazz, parentClass, childClass, childFilter, parents, foreignField, targetField, database);
    }

    private class FilteredJoinedIterator implements Iterator<T> {

        private final Class<T> clazz;
        private final Class<P> parentClass;
        private final Class<C> childClass;
        private final Filter childFilter;
        private final Iterator<Long> parents;
        private final String foreignField;
        private final String targetField;
        private final NitriteDatabase database;
        private T next;

        public FilteredJoinedIterator(Class<T> clazz, Class<P> parentClass, Class<C> childClass, Filter childFilter, Set<Long> parents, String foreignField, String targetField, NitriteDatabase database) {
            this.clazz = clazz;
            this.parentClass = parentClass;
            this.childClass = childClass;
            this.childFilter = childFilter;

            try {
                Iterable<C> objectCursor = database.find(childFilter, childClass);
                Field field = objectCursor.getClass().getDeclaredField("cursor");
                field.setAccessible(true);
                org.dizitart.no2.Cursor docCursor = (org.dizitart.no2.Cursor) field.get(objectCursor);

                Document projection = createDocument(foreignField, null);
                RecordIterable<Document> documents = docCursor.project(projection);
                Set<Long> pIds = StreamSupport.stream(documents.spliterator(), false).mapToLong(doc -> (Long) doc.get(foreignField)).boxed().collect(Collectors.toSet());

                parents.retainAll(pIds);
            } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            this.parents = parents.iterator();
            this.foreignField = foreignField;
            this.targetField = targetField;
            this.database = database;
            this.next = null;
        }

        @Override
        public boolean hasNext() {
            if (next == null) {
                next = advance();
            }
            return next != null;
        }

        @Override
        public T next() {
            if (next == null) {
                next = advance();
            }
            T result = next;
            next = null;
            return result;
        }

        public T advance() {
            try {
                T result = null;
                while (result == null && parents.hasNext()) {
                    long id = parents.next();
                    Iterable<P> parent = database.find(new Filter().eq("id", id), parentClass);
                    Filter cFilter = new Filter().and().eq(foreignField, id);
                    cFilter.filterChain.addAll(childFilter.filterChain);
                    Cursor<C> children = (Cursor<C>) database.find(cFilter, childClass);
                    if (children.size() == 0) {
                        continue;
                    }

                    Lookup lookup = new Lookup();
                    lookup.setLocalField("id");
                    lookup.setForeignField(foreignField);
                    lookup.setTargetField(targetField);

                    RecordIterable<T> res = ((Cursor<P>) parent).join(children, lookup, clazz);
                    result = res.firstOrDefault();
                }
                return result;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
