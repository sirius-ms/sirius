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

package de.unijena.bioinf.storage.db.nosql.nitrite.projection;

import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import org.apache.commons.lang3.ClassUtils;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.common.RecordStream;
import org.dizitart.no2.common.mapper.NitriteMapper;
import org.dizitart.no2.exceptions.InvalidOperationException;
import org.dizitart.no2.filters.FluentFilter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public class InjectedObjectStream<T> implements RecordStream<T> {
    private final Iterable<T> objects;

    private final Set<String> injectedFields;

    private final NitriteCollection collection;

    private final Field primaryKeyField;

    private final NitriteMapper nitriteMapper;

    public InjectedObjectStream(Iterable<T> objects, Set<String> injectedFields, NitriteCollection collection, Field primaryKeyField, NitriteMapper nitriteMapper) {
        this.objects = objects;
        this.injectedFields = injectedFields;
        this.collection = collection;
        this.primaryKeyField = primaryKeyField;
        this.nitriteMapper = nitriteMapper;
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<T> iterator = this.objects == null ? Collections.emptyIterator() : this.objects.iterator();
        return new InjectedObjectStream<T>.InjectedObjectIterator(iterator, this.injectedFields, this.collection, this.primaryKeyField, this.nitriteMapper);
    }

    public String toString() {
        return this.toList().toString();
    }

    private class InjectedObjectIterator implements Iterator<T> {
        private final Iterator<T> iterator;
        private T nextElement = null;
        private final Set<String> injectedFields;

        private final NitriteCollection collection;

        private final Field primaryKeyField;

        private final NitriteMapper nitriteMapper;

        InjectedObjectIterator(Iterator<T> iterator, Set<String> injectedFields, NitriteCollection collection, Field primaryKeyField, NitriteMapper nitriteMapper) {
            this.iterator = iterator;
            this.injectedFields = injectedFields;
            this.collection = collection;
            this.primaryKeyField = primaryKeyField;
            this.nitriteMapper = nitriteMapper;
            this.nextMatch();
        }

        public boolean hasNext() {
            return this.nextElement != null;
        }

        public T next() {
            T returnValue = this.nextElement;
            this.nextMatch();
            return returnValue;
        }

        private void nextMatch() {
            while(true) {
                if (this.iterator.hasNext()) {
                    T next = this.iterator.next();
                    if (next == null) {
                        continue;
                    }

                    try {
                        this.nextElement = inject(next, this.injectedFields, this.collection, this.primaryKeyField, this.nitriteMapper);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                this.nextElement = null;
                return;
            }
        }

        public void remove() {
            throw new InvalidOperationException("Remove on a cursor is not supported");
        }

    }

    private static <T> void inject(T object, Document document, Class<?> clazz, Set<String> injectedFields, NitriteMapper nitriteMapper) {
        for (String fieldName : injectedFields) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals(fieldName)) {
                    try {
                        field.setAccessible(true);
                        Object content = document.get(field.getName());
                        if (content instanceof Document fieldDoc) {
                            field.set(object, nitriteMapper.tryConvert(fieldDoc, field.getType()));
                        } else {
                            field.set(object, document.get(field.getName(), field.getType()));
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public static <T> T inject(T original, Set<String> injectedFields, NitriteCollection collection, Field primaryKeyField, NitriteMapper nitriteMapper) throws IOException {
        if (injectedFields.isEmpty()) return original;

        Object pkValue = NitriteDatabase.getPrimaryKeyValue(original, primaryKeyField).orElseThrow(() -> new IOException("id filter can not be null"));
        Document fromDB = collection.find(FluentFilter.where(primaryKeyField.getName()).eq(pkValue)).firstOrNull();
        if (fromDB == null) {
            throw new IOException("Document not found: " + pkValue);
        }

        inject(original, fromDB, original.getClass(), injectedFields, nitriteMapper);
        for (Class<?> clazz : ClassUtils.getAllSuperclasses(original.getClass())) {
            inject(original, fromDB, clazz, injectedFields, nitriteMapper);
        }
        return original;
    }

}
