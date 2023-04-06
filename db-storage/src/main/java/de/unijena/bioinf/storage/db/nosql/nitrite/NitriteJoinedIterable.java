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
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.dizitart.no2.Lookup;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.RecordIterable;
import org.dizitart.no2.objects.Cursor;
import org.dizitart.no2.objects.Id;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.StreamSupport;

public class NitriteJoinedIterable<T, P, C> implements Iterable<T> {

    protected final Class<T> clazz;
    protected final Class<P> parentClass;
    protected final Class<C> childClass;
    protected LongSet parents;
    protected final String foreignField;
    protected final String targetField;

    protected final NitriteDatabase database;

    public static <T, P, C> NitriteJoinedIterable<T, P, C> newInstance(Class<T> clazz, Class<P> parentClass, Class<C> childClass, Iterable<P> parents, String foreignField, String targetField, NitriteDatabase database) {
        NitriteJoinedIterable<T, P, C> iterable = new NitriteJoinedIterable<>(clazz, parentClass, childClass, foreignField, targetField, database);
        iterable.initParentIds(parents);
        return iterable;
    }

    protected NitriteJoinedIterable(Class<T> clazz, Class<P> parentClass, Class<C> childClass, String foreignField, String targetField, NitriteDatabase database) {
        this.clazz = clazz;
        this.parentClass = parentClass;
        this.childClass = childClass;
        this.foreignField = foreignField;
        this.targetField = targetField;
        this.database = database;
    }

    protected void initParentIds(Iterable<P> parents) {
        if (parents instanceof Cursor) {
            this.parents = new LongArraySet(((Cursor<P>) parents).size());
            ((Cursor<P>) parents).idSet().stream().mapToLong(NitriteId::getIdValue).forEach((LongConsumer) this.parents::add);
        } else if (parents instanceof org.dizitart.no2.Cursor) {
            this.parents = new LongArraySet(((org.dizitart.no2.Cursor) parents).size());
            ((org.dizitart.no2.Cursor) parents).idSet().stream().mapToLong(NitriteId::getIdValue).forEach((LongConsumer) this.parents::add);
        } else {
            this.parents = new LongArraySet();
            StreamSupport.stream(parents.spliterator(), false).mapToLong(p -> {
                try {
                    if (p instanceof NitritePOJO) {
                        return ((NitritePOJO) p).getId().getIdValue();
                    } else {
                        for (Field field : p.getClass().getDeclaredFields()) {
                            if (field.isAnnotationPresent(Id.class)) {
                                field.setAccessible(true);
                                Object fieldValue = field.get(p);
                                if (fieldValue instanceof NitriteId) {
                                    return ((NitriteId) fieldValue).getIdValue();
                                } else if (fieldValue instanceof Long) {
                                    return (long) fieldValue;
                                }
                            }
                        }
                        throw new RuntimeException("Object has no id.");
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }).forEach((LongConsumer) this.parents::add);
        }
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        if (parents.isEmpty()) {
            return Collections.emptyIterator();
        }
        return new JoinedIterator(clazz, parentClass, childClass, parents, foreignField, targetField, database);
    }

    protected class JoinedIterator implements Iterator<T> {

        protected final Class<T> clazz;
        protected final Class<P> parentClass;
        protected final Class<C> childClass;
        protected LongIterator parents;
        protected final String foreignField;
        protected final String targetField;
        protected final NitriteDatabase database;

        public JoinedIterator(Class<T> clazz, Class<P> parentClass, Class<C> childClass, LongSet parents, String foreignField, String targetField, NitriteDatabase database) {
            this.clazz = clazz;
            this.parentClass = parentClass;
            this.childClass = childClass;
            this.parents = parents.iterator();
            this.foreignField = foreignField;
            this.targetField = targetField;
            this.database = database;
        }

        @Override
        public boolean hasNext() {
            return parents.hasNext();
        }

        @Override
        public T next() {
            try {
                long id = parents.nextLong();
                Iterable<P> parent = database.find(new Filter().eq("id", id), parentClass);
                Iterable<C> children = database.find(new Filter().eq(foreignField, id), childClass);

                Lookup lookup = new Lookup();
                lookup.setLocalField("id");
                lookup.setForeignField(foreignField);
                lookup.setTargetField(targetField);

                RecordIterable<T> res = ((org.dizitart.no2.objects.Cursor<P>) parent).join((org.dizitart.no2.objects.Cursor<C>) children, lookup, clazz);
                return res.firstOrDefault();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
