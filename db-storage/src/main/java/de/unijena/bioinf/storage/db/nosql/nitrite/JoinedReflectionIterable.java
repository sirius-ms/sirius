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
import org.apache.commons.lang3.reflect.FieldUtils;
import org.dizitart.no2.Document;
import org.dizitart.no2.mapper.NitriteMapper;
import org.dizitart.no2.objects.Cursor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

public class JoinedReflectionIterable<P, C> implements Iterable<P> {

    protected JoinedIterable<P, P> objectIterable = null;

    protected Iterable<P> parents = null;

    protected Class<C> childClass;

    protected Class<P> parentClass;

    protected Function<Object, Iterable<Document>> children;

    protected String localField;

    protected String targetField;

    protected NitriteMapper mapper;

    @SuppressWarnings("unchecked")
    public JoinedReflectionIterable(
            Class<C> childClass,
            Iterable<P> parents,
            Function<Object, Iterable<Document>> children,
            String localField,
            String targetField,
            NitriteMapper mapper
    ) throws IOException {
        if (parents instanceof Cursor) {
            if (((Cursor<P>) parents).size() > 0) {
                P first = ((Cursor<P>) parents).firstOrDefault();
                objectIterable = new JoinedIterable<>((Class<P>) first.getClass(), parents, children, localField, targetField, mapper);
            }
        } else {
            if (parents.iterator().hasNext()) {
                this.parentClass = (Class<P>) parents.iterator().next().getClass();
                this.childClass = childClass;
                this.parents = parents;
                this.children = children;
                this.localField = localField;
                this.targetField = targetField;
                this.mapper = mapper;
            }
        }

    }

    @NotNull
    @Override
    public Iterator<P> iterator() {
        if (objectIterable != null) {
            return objectIterable.iterator();
        } else if (parents != null) {
            try {
                return new JoinedReflectionIterator<>(parentClass, childClass, parents, children, localField, targetField, mapper);
            } catch (NoSuchFieldException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return Collections.emptyIterator();
        }
    }

    static class JoinedReflectionIterator<P, C> implements Iterator<P> {

        protected final Class<C> childClass;

        protected final Iterator<P> parentIterator;

        protected final Function<Object, Iterable<Document>> children;

        protected final String localField;

        protected final Field targetField;

        protected final NitriteMapper mapper;

        JoinedReflectionIterator(Class<P> parentClass, Class<C> childClass, Iterable<P> parents, Function<Object, Iterable<Document>> children, String localField, String targetField, NitriteMapper mapper) throws NoSuchFieldException, IOException {
            this.childClass = childClass;
            this.parentIterator = parents.iterator();
            this.children = children;
            this.localField = localField;
            this.targetField = parentClass.getDeclaredField(targetField);
            if (!ClassUtils.getAllInterfaces(this.targetField.getType()).contains(Collection.class)) {
                throw new IOException("targetField must be a collection.");
            }
            parentClass.getModule().addOpens(parentClass.getPackageName(), getClass().getModule());
            this.targetField.setAccessible(true);

            this.mapper = mapper;
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
                Object localObject = localF.get(target);

                Set<C> targetChildren = new HashSet<>();
                for (Document foreignDoc : children.apply(localObject)) {
                    targetChildren.add(mapper.asObject(foreignDoc, childClass));
                }
                if (!targetChildren.isEmpty()) {
                    Collection<C> collection = (Collection<C>) targetField.get(target);
                    if (collection == null) {
                        if (targetField.getType() == List.class) {
                            collection = new ArrayList<>(targetChildren);
                        } else if (targetField.getType() == BlockingDeque.class) {
                            collection = new LinkedBlockingDeque<>();
                        } else if (targetField.getType() == BlockingQueue.class) {
                            collection = new LinkedBlockingQueue<>();
                        } else if (targetField.getType() == Deque.class || targetField.getType() == Queue.class) {
                            collection = new ArrayDeque<>();
                        } else if (targetField.getType() == Set.class) {
                            collection = new HashSet<>();
                        } else if (targetField.getType() == SortedSet.class) {
                            collection = new TreeSet<>();
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
