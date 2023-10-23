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

package de.unijena.bioinf.storage.db.nosql;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static de.unijena.bioinf.storage.db.nosql.utils.ExtFieldUtils.getAllField;
import static de.unijena.bioinf.storage.db.nosql.utils.ExtFieldUtils.getAllFieldValue;

public interface Database<DocType> extends Closeable, AutoCloseable {

    enum SortOrder {
        ASCENDING, DESCENDING
    }

    Path location();

    //region CRUD operations

    <T> int insert(T object) throws IOException;

    <T> int insertAll(Iterable<T> objects) throws IOException;

    int insert(String collectionName, DocType document) throws IOException;

    int insertAll(String collectionName, Iterable<DocType> documents) throws IOException;

    <T> int upsert(T object) throws IOException;

    <T> int upsertAll(Iterable<T> objects) throws IOException;

    int upsert(String collectionName, DocType document) throws IOException;

    int upsertAll(String collectionName, Iterable<DocType> documents) throws IOException;

    <T> T getById(long id, Class<T> clazz, String... withOptionalFields) throws IOException;

    DocType getById(String collectionName, long id, String... withOptionalFields) throws IOException;

    <T> Iterable<T> find(Filter filter, Class<T> clazz, String... withOptionalFields) throws IOException;

    <T> Iterable<T> find(Filter filter, Class<T> clazz, int offset, int pageSize, String... withOptionalFields) throws IOException;

    <T> Iterable<T> find(Filter filter, Class<T> clazz, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    <T> Iterable<T> find(Filter filter, Class<T> clazz, int offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    Iterable<DocType> find(String collectionName, Filter filter, String... withOptionalFields) throws IOException;

    Iterable<DocType> find(String collectionName, Filter filter, int offset, int pageSize, String... withOptionalFields) throws IOException;

    Iterable<DocType> find(String collectionName, Filter filter, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    Iterable<DocType> find(String collectionName, Filter filter, int offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, String... withOptionalFields) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, int offset, int pageSize, String... withOptionalFields) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, int offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    Iterable<DocType> findAll(String collectionName, String... withOptionalFields) throws IOException;

    Iterable<DocType> findAll(String collectionName, int offset, int pageSize, String... withOptionalFields) throws IOException;

    Iterable<DocType> findAll(String collectionName, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    Iterable<DocType> findAll(String collectionName, int offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    <T> T injectOptionalFields(T object, String... optionalFields) throws IOException;

    DocType injectOptionalFields(String collectionName, DocType document, String... optionalFields) throws IOException;

    <T> Iterable<T> injectOptionalFields(Class<T> clazz, Iterable<T> objects, String... optionalFields) throws IOException;

    Iterable<DocType> injectOptionalFields(String collectionName, Iterable<DocType> documents, String... optionalFields) throws IOException;

    <P, C> Iterable<P> joinAllChildren(Class<C> childClass, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException;

    <P, C> Iterable<P> joinChildren(Class<C> childClass, Filter childFilter, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException;

    <T, P, C> Iterable<T> joinAllChildren(Class<T> targetClass, Class<C> childClass, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException;

    <T, P, C> Iterable<T> joinChildren(Class<T> targetClass, Class<C> childClass, Filter childFilter, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException;

    Iterable<DocType> joinAllChildren(String childCollection, Iterable<DocType> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException;

    Iterable<DocType> joinChildren(String childCollectionName, Filter childFilter, Iterable<DocType> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException;

    //todo @MEL implement stream based join/fetch api for documents as well.
    default <P, C> P fetchChild(final P parent, String matchingField, String targetField, Class<C> childClass, String... withOptionalChildFields) throws IOException {
        return fetchChild(parent, matchingField, matchingField, targetField, childClass, withOptionalChildFields);
    }

    default <P, C> P fetchChild(final P parent, String localField, String foreignField, String targetField, Class<C> childClass, String... withOptionalChildFields) throws IOException {
        try {
            Object matchingValue = getAllFieldValue(parent, localField);
            Field target = getAllField(parent.getClass(), targetField);

            List<C> targetChildren =
                    findStr(Filter.build().eq(foreignField, matchingValue), childClass, withOptionalChildFields)
                            .toList();

            if (targetChildren.isEmpty())
                target.set(parent, null);
            else if (targetChildren.size() == 1)
                target.set(parent, targetChildren.get(0));
            else
                throw new IllegalStateException("Multiple matching children objects found but single candidate expected");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return parent;
    }

    private static <P, C> P fetchChildren(final P parent, String targetField, Collection<? extends C> targetChildren) throws IOException {
        try {
            //todo @MEL can we call join here. This might be duplicated code with the Joining interator.
            final Field field = getAllField(parent.getClass(), targetField);

            if (!targetChildren.isEmpty()) {
                Collection<C> collection = (Collection<C>) field.get(parent);
                if (collection == null) {
                    if (field.getType() == List.class) {
                        collection = new ArrayList<>(targetChildren);
                    } else if (field.getType() == BlockingDeque.class) {
                        collection = new LinkedBlockingDeque<>();
                    } else if (field.getType() == BlockingQueue.class) {
                        collection = new LinkedBlockingQueue<>();
                    } else if (field.getType() == Deque.class || field.getType() == Queue.class) {
                        collection = new ArrayDeque<>();
                    } else if (field.getType() == Set.class) {
                        collection = new HashSet<>();
                    } else if (field.getType() == SortedSet.class) {
                        collection = new TreeSet<>();
                    } else if (field.getType() == TransferQueue.class) {
                        collection = new LinkedTransferQueue<>();
                    } else {
                        collection = (Collection<C>) field.getType().getDeclaredConstructor().newInstance();
                    }
                }
                collection.addAll(targetChildren);
                field.set(parent, collection);
            }
            return parent;
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }


    default <P, C> P fetchChildren(final P parent, String targetField, Filter childFilter, Class<C> childClass, String... withOptionalChildFields) throws IOException {
        List<C> targetChildren = findStr(childFilter, childClass, withOptionalChildFields).toList();
        fetchChildren(parent, targetField, targetChildren);
        return parent;
    }

    default <P, C> P fetchAllChildren(final P parent, String matchingField, String targetField, Class<C> childClass, String... withOptionalChildFields) throws IOException {
        return fetchAllChildren(parent, matchingField, matchingField, targetField, childClass, withOptionalChildFields);
    }

    default <P, C> P fetchAllChildren(final P parent, String localField, String foreignField, String targetField, Class<C> childClass, String... withOptionalChildFields) throws IOException {
        try {
            Object matchingValue = getAllFieldValue(parent, localField);

            List<C> targetChildren =
                    findStr(Filter.build().eq(foreignField, matchingValue), childClass, withOptionalChildFields)
                            .toList();

            fetchChildren(parent, targetField, targetChildren);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return parent;
    }

    <T> int count(Filter filter, Class<T> clazz) throws IOException;

    <T> int count(Filter filter, Class<T> clazz, int offset, int pageSize) throws IOException;

    <T> int countAll(Class<T> clazz) throws IOException;

    int count(String collectionName, Filter filter) throws IOException;

    int count(String collectionName, Filter filter, int offset, int pageSize) throws IOException;

    int countAll(String collectionName) throws IOException;

    <T> int remove(T object) throws IOException;

    <T> int removeAll(Iterable<T> objects) throws IOException;

    <T> int removeAll(Filter filter, Class<T> clazz) throws IOException;

    int remove(String collectionName, DocType document) throws IOException;

    int removeAll(String collectionName, Iterable<DocType> documents) throws IOException;

    int removeAll(String collectionName, Filter filter) throws IOException;

    //endregion
    <ObjectFilterType> ObjectFilterType getObjectFilter(Filter filter);

    <FilterType> FilterType getFilter(Filter filter);


    // region CRUD Streaming API
    default <T> Stream<T> findStr(Filter filter, Class<T> clazz, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(filter, clazz, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findStr(Filter filter, Class<T> clazz, int offset, int pageSize, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(filter, clazz, offset, pageSize, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findStr(Filter filter, Class<T> clazz, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(filter, clazz, sortField, sortOrder, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findStr(Filter filter, Class<T> clazz, int offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(filter, clazz, offset, pageSize, sortField, sortOrder, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findStr(String collectionName, Filter filter, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(collectionName, filter, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findStr(String collectionName, Filter filter, int offset, int pageSize, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(collectionName, filter, offset, pageSize, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findStr(String collectionName, Filter filter, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(collectionName, filter, sortField, sortOrder, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findStr(String collectionName, Filter filter, int offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(collectionName, filter, offset, pageSize, sortField, sortOrder, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findAllStr(Class<T> clazz, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(clazz, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findAllStr(Class<T> clazz, int offset, int pageSize, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(clazz, offset, pageSize, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findAllStr(Class<T> clazz, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(clazz, sortField, sortOrder, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findAllStr(Class<T> clazz, int offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(clazz, offset, pageSize, sortField, sortOrder, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findAllStr(String collectionName, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(collectionName, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findAllStr(String collectionName, int offset, int pageSize, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(collectionName, offset, pageSize, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findAllStr(String collectionName, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(collectionName, sortField, sortOrder, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findAllStr(String collectionName, int offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(collectionName, offset, pageSize, sortField, sortOrder, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> injectOptionalFieldsStr(Class<T> clazz, Iterable<T> objects, String... optionalFields) throws IOException {
        return StreamSupport.stream(injectOptionalFields(clazz, objects, optionalFields).spliterator(), false);
    }

    default <T> Stream<T> injectOptionalFieldsStr(Class<T> clazz, Stream<T> objects, String... optionalFields) throws IOException {
        return StreamSupport.stream(injectOptionalFields(clazz, objects.toList(), optionalFields).spliterator(), false);
    }

    default Stream<DocType> injectOptionalFieldsStr(String collectionName, Iterable<DocType> documents, String... optionalFields) throws IOException {
        return StreamSupport.stream(injectOptionalFields(collectionName, documents, optionalFields).spliterator(), false);
    }

    default Stream<DocType> injectOptionalFieldsStr(String collectionName, Stream<DocType> documents, String... optionalFields) throws IOException {
        return StreamSupport.stream(injectOptionalFields(collectionName, documents.toList(), optionalFields).spliterator(), false);
    }

    default <P, C> Stream<P> joinAllChildrenStr(Class<C> childClass, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return StreamSupport.stream(joinAllChildren(childClass, parents, localField, foreignField, targetField, withOptionalChildFields).spliterator(), false);
    }

    default <P, C> Stream<P> joinChildrenStr(Class<C> childClass, Filter childFilter, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return StreamSupport.stream(joinChildren(childClass, childFilter, parents, localField, foreignField, targetField, withOptionalChildFields).spliterator(), false);
    }

    default <T, P, C> Stream<T> joinAllChildrenStr(Class<T> targetClass, Class<C> childClass, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return StreamSupport.stream(joinAllChildren(targetClass, childClass, parents, localField, foreignField, targetField, withOptionalChildFields).spliterator(), false);
    }

    default <T, P, C> Stream<T> joinChildrenStr(Class<T> targetClass, Class<C> childClass, Filter childFilter, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return StreamSupport.stream(joinChildren(targetClass, childClass, childFilter, parents, localField, foreignField, targetField, withOptionalChildFields).spliterator(), false);
    }

    default Stream<DocType> joinAllChildrenStr(String childCollectionName, Iterable<DocType> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return StreamSupport.stream(joinAllChildren(childCollectionName, parents, localField, foreignField, targetField, withOptionalChildFields).spliterator(), false);
    }

    default Stream<DocType> joinChildrenStr(String childCollectionName, Filter childFilter, Iterable<DocType> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return StreamSupport.stream(joinChildren(childCollectionName, childFilter, parents, localField, foreignField, targetField, withOptionalChildFields).spliterator(), false);
    }
    //endregion
}
