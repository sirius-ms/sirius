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

import org.apache.commons.lang3.ClassUtils;
import org.dizitart.no2.Document;
import org.dizitart.no2.NitriteCollection;
import org.dizitart.no2.exceptions.InvalidOperationException;
import org.dizitart.no2.filters.Filters;
import org.dizitart.no2.mapper.NitriteMapper;
import org.dizitart.no2.objects.ObjectRepository;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;

import static org.dizitart.no2.exceptions.ErrorMessage.ID_FILTER_VALUE_CAN_NOT_BE_NULL;
import static org.dizitart.no2.exceptions.ErrorMessage.REMOVE_ON_DOCUMENT_ITERATOR_NOT_SUPPORTED;

public class InjectingIterable<T> implements Iterable<T> {

    private final Iterable<T> parent;

    private final Set<String> injectedFields;

    private final NitriteCollection collection;

    private final Field primaryKeyField;

    private final NitriteMapper mapper;

    public InjectingIterable(Iterable<T> parent, Set<String> injectedFields, ObjectRepository<T> repository, Field primaryKeyField, NitriteMapper mapper) {
        this.collection = repository.getDocumentCollection();
        this.parent = parent;
        this.injectedFields = injectedFields;
        this.primaryKeyField = primaryKeyField;
        this.mapper = mapper;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new InjectingIterator();
    }

    private class InjectingIterator implements Iterator<T> {
        private final Iterator<T> iterator;
        private T nextElement = null;

        InjectingIterator() {
            iterator = parent.iterator();
            nextMatch();
        }

        @Override
        public boolean hasNext() {
            return nextElement != null;
        }

        @Override
        public T next() {
            T returnValue = nextElement;
            nextMatch();
            return returnValue;
        }

        private void nextMatch() {
            while (iterator.hasNext()) {
                T object = iterator.next();
                if (object != null) {
                    try {
                        nextElement = inject(object, injectedFields, collection, primaryKeyField, mapper);
                        return;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    nextElement = null;
                }
            }

            nextElement = null;
        }

        @Override
        public void remove() {
            throw new InvalidOperationException(REMOVE_ON_DOCUMENT_ITERATOR_NOT_SUPPORTED);
        }

    }

    private static <T> void inject(T object, Document document, Class<?> clazz, Set<String> injectedFields, NitriteMapper mapper) {
        for (String fieldName : injectedFields) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals(fieldName)) {
                    try {
                        field.setAccessible(true);
                        Object content = document.get(field.getName());
                        if (content instanceof Document) {
                            field.set(object, mapper.asObject((Document) content, field.getType()));
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

    public static <T> T inject(T original, Set<String> injectedFields, NitriteCollection collection, Field primaryKeyField, NitriteMapper mapper) throws IOException {
        if (injectedFields.isEmpty()) return original;

        Object pkValue = NitriteDatabase.getFieldValue(original, primaryKeyField).orElseThrow(() -> new IOException(ID_FILTER_VALUE_CAN_NOT_BE_NULL.getMessage()));
        Document fromDB = collection.find(Filters.eq(primaryKeyField.getName(), pkValue)).firstOrDefault();
        if (fromDB == null) {
            throw new IOException("Document not found: " + pkValue);
        }

        inject(original, fromDB, original.getClass(), injectedFields, mapper);
        for (Class<?> clazz : ClassUtils.getAllSuperclasses(original.getClass())) {
            inject(original, fromDB, clazz, injectedFields, mapper);
        }
        return original;
    }

}
