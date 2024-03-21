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

package de.unijena.bioinf.storage.db.nosql.nitrite.joining;

import de.unijena.bioinf.storage.db.nosql.utils.ExtFieldUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.common.mapper.NitriteMapper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

public class JoinedReflectionIterable<P, C> implements Iterable<P> {

    private final Class<C> childClass;

    private final Iterable<P> parents;

    private final Class<P> parentClass;

    private final Function<Object, Iterable<Document>> children;

    private final String localField;

    private final String targetField;

    private final NitriteMapper mapper;

    @SuppressWarnings("unchecked")
    public JoinedReflectionIterable(Class<C> childClass, Iterable<P> parents, Function<Object, Iterable<Document>> children, String localField, String targetField, NitriteMapper mapper) {
        this.childClass = childClass;
        this.parents = parents;
        this.parentClass = (parents.iterator().hasNext()) ? (Class<P>) parents.iterator().next().getClass() : null;
        this.children = children;
        this.localField = localField;
        this.targetField = targetField;
        this.mapper = mapper;
    }

    @NotNull
    @Override
    public Iterator<P> iterator() {
        try {
            return new JoinedReflectionIterator<>(parentClass, childClass, parents, children, localField, targetField, mapper);
        } catch (NoSuchFieldException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class JoinedReflectionIterator<P, C> implements Iterator<P> {

        private final Class<C> childClass;

        private final Iterator<P> parentIterator;

        private final Function<Object, Iterable<Document>> children;

        private final String localField;

        private final Field targetField;

        private final NitriteMapper mapper;

        JoinedReflectionIterator(Class<P> parentClass, Class<C> childClass, Iterable<P> parents, Function<Object, Iterable<Document>> children, String localField, String targetField, NitriteMapper mapper) throws NoSuchFieldException, IOException {
            this.childClass = childClass;
            this.parentIterator = parents.iterator();
            this.children = children;
            this.localField = localField;
            this.mapper = mapper;
            this.targetField = ExtFieldUtils.getAllField(parentClass, targetField);
            if (!ClassUtils.getAllInterfaces(this.targetField.getType()).contains(Collection.class)) {
                throw new IOException("targetField must be a collection.");
            }
            this.targetField.setAccessible(true);
        }

        @Override
        public boolean hasNext() {
            return parentIterator.hasNext();
        }

        @Override
        @SuppressWarnings("unchecked")
        public P next() {
            try {
                P target = parentIterator.next();
                Field localF = null;
                for (Field field : FieldUtils.getAllFields(target.getClass())) {
                    if (field.getName().equals(localField)) {
                        localF = field;
                        break;
                    }
                }
                if (localF == null) {
                    throw new NoSuchFieldException(localField);
                }
                localF.setAccessible(true);
                Object localObject = localF.get(target);

                Set<C> targetChildren = new HashSet<>();
                for (Document foreignDoc : children.apply(localObject)) {
                    targetChildren.add((C) mapper.tryConvert(foreignDoc, childClass));
                }
                if (!targetChildren.isEmpty()) {
                    Collection<C> collection = (Collection<C>) targetField.get(target);
                    if (collection == null) {
                        if (targetField.getType() == List.class) {
                            collection = new ArrayList<>();
                        } else if (targetField.getType() == BlockingDeque.class) {
                            collection = new LinkedBlockingDeque<>();
                        } else if (targetField.getType() == BlockingQueue.class) {
                            collection = new LinkedBlockingQueue<>();
                        } else if (targetField.getType() == Deque.class || targetField.getType() == Queue.class) {
                            collection = new ArrayDeque<>();
                        } else if (targetField.getType() == Set.class) {
                            collection = new HashSet<>();
                        } else if (targetField.getType() == TransferQueue.class) {
                            collection = new LinkedTransferQueue<>();
                        } else {
                            collection = (Collection<C>) targetField.getType().getDeclaredConstructor().newInstance();
                        }
                    }
                    collection.addAll(targetChildren);
                    targetField.set(target, collection);
                }
                return target;
            } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InstantiationException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
