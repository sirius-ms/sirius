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
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongConsumer;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.dizitart.no2.Document;
import org.dizitart.no2.Lookup;
import org.dizitart.no2.RecordIterable;
import org.dizitart.no2.objects.Cursor;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.StreamSupport;

import static org.dizitart.no2.Document.createDocument;

public class NitriteFilteredJoinedIterable<T, P, C> extends NitriteJoinedIterable<T, P, C> {

    protected final Filter childFilter;

    public static <T, P, C> NitriteFilteredJoinedIterable<T, P, C> newInstance(Class<T> clazz, Class<P> parentClass, Class<C> childClass, Filter childFilter, Iterable<P> parents, String foreignField, String targetField, NitriteDatabase database) throws IOException {
        NitriteFilteredJoinedIterable<T, P, C> iterable = new NitriteFilteredJoinedIterable<>(clazz, parentClass, childClass, childFilter, foreignField, targetField, database);
        iterable.initParentIds(parents);

        try {
            Iterable<C> objectCursor = database.find(childFilter, childClass);
            Field field = objectCursor.getClass().getDeclaredField("cursor");
            field.setAccessible(true);
            org.dizitart.no2.Cursor docCursor = (org.dizitart.no2.Cursor) field.get(objectCursor);

            Document projection = createDocument(foreignField, null);
            RecordIterable<Document> documents = docCursor.project(projection);
            LongSet pIds = new LongArraySet(documents.size());
            StreamSupport.stream(documents.spliterator(), false).mapToLong(doc -> (Long) doc.get(foreignField)).forEach((LongConsumer) pIds::add);

            iterable.parents.retainAll(pIds);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return iterable;
    }

    protected NitriteFilteredJoinedIterable(Class<T> clazz, Class<P> parentClass, Class<C> childClass, Filter childFilter, String foreignField, String targetField, NitriteDatabase database) {
        super(clazz, parentClass, childClass, foreignField, targetField, database);
        this.childFilter = childFilter;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        if (this.parents.isEmpty()) {
            return Collections.emptyIterator();
        }
        return new FilteredJoinedIterator(clazz, parentClass, childClass, childFilter, parents, foreignField, targetField, database);
    }

    protected class FilteredJoinedIterator extends JoinedIterator {

        private final Filter childFilter;
        private T next;

        public FilteredJoinedIterator(Class<T> clazz, Class<P> parentClass, Class<C> childClass, Filter childFilter, LongSet parents, String foreignField, String targetField, NitriteDatabase database) {
            super(clazz, parentClass, childClass, parents, foreignField, targetField, database);
            this.childFilter = childFilter;
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
                    long id = parents.nextLong();
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
