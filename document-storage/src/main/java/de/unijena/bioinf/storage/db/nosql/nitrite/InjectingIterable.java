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
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.exceptions.InvalidOperationException;
import org.dizitart.no2.mapper.NitriteMapper;
import org.dizitart.no2.objects.ObjectRepository;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;

import static org.dizitart.no2.exceptions.ErrorMessage.REMOVE_ON_DOCUMENT_ITERATOR_NOT_SUPPORTED;

public class InjectingIterable<T> implements Iterable<T> {

    private final Iterable<T> parent;

    private final Set<String> injectedFields;

    private final NitriteCollection collection;

    private final String idField;

    private final NitriteMapper mapper;

    public InjectingIterable(Iterable<T> parent, Set<String> injectedFields, ObjectRepository<T> repository, String idField, NitriteMapper mapper) throws IOException {
        try {
            Field cField = repository.getClass().getDeclaredField("collection");
            cField.setAccessible(true);
            this.collection = (NitriteCollection) cField.get(repository);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IOException(e);
        }
        this.parent = parent;
        this.injectedFields = injectedFields;
        this.idField = idField;
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
                    T injected = inject(object, injectedFields, collection, idField, mapper);
                    nextElement = injected;
                    return;
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

    public static <T> T inject(T original, Set<String> injectedFields, NitriteCollection collection, String idField, NitriteMapper mapper) {
        if (injectedFields.size() == 0) return original;

        long id;
        try {
            Field idf = original.getClass().getDeclaredField(idField);
            idf.setAccessible(true);
            id = idf.getLong(original);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Document fromDB = collection.getById(NitriteId.createId(id));
        if (fromDB == null) {
            throw new RuntimeException("No such document: " + id);
        }

        inject(original, fromDB, original.getClass(), injectedFields, mapper);
        for (Class<?> clazz : ClassUtils.getAllSuperclasses(original.getClass())) {
            inject(original, fromDB, clazz, injectedFields, mapper);
        }
        return original;
    }

}
