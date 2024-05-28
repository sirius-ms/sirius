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

import org.apache.commons.io.function.IORunnable;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static de.unijena.bioinf.storage.db.nosql.utils.ExtFieldUtils.getAllField;
import static de.unijena.bioinf.storage.db.nosql.utils.ExtFieldUtils.getAllFieldValue;

public interface Database<DocType> extends Closeable, AutoCloseable {
    void disableIndices(Class<?> clazz, Index... keep);

    void enableIndices(Class<?> clazz);

    //todo do we want to change from IO to Runtimeexceptions for better lamda compatibility
    enum SortOrder {
        ASCENDING, DESCENDING
    }

    Path location();

    //force to write data to disk
    void flush();

    //region CRUD operations

    <T> int insert(T object) throws IOException;

    <T> int insertAll(Iterable<T> objects) throws IOException;

    int insert(String collectionName, DocType document) throws IOException;

    int insertAll(String collectionName, Iterable<DocType> documents) throws IOException;

    <T> int upsert(T object) throws IOException;

    default <T> int modify(Object primaryKey, Class<T> clazz, Function<T, T> modifier) throws IOException {
        //if we move this to database implementation level this could be a transaction.
        T obj = getByPrimaryKey(primaryKey, clazz).orElseThrow(() -> new IllegalArgumentException("Entity to modify not found"));
        obj = modifier.apply(obj);
        return upsert(obj);
    }

    default <T> int modify(Object primaryKey, Class<T> clazz, Consumer<T> modifier) throws IOException {
        return modify(primaryKey, clazz, t -> {
            modifier.accept(t);
            return t;
        });
    }

    <T> int upsertAll(Iterable<T> objects) throws IOException;

    int upsert(String collectionName, DocType document) throws IOException;

    int upsertAll(String collectionName, Iterable<DocType> documents) throws IOException;

    <T> Optional<T> getByPrimaryKey(Object primaryKey, Class<T> clazz, String... withOptionalFields) throws IOException;

    <T> Iterable<T> find(Filter filter, Class<T> clazz, String... withOptionalFields) throws IOException;

    <T> Iterable<T> find(Filter filter, Class<T> clazz, long offset, int pageSize, String... withOptionalFields) throws IOException;

    <T> Iterable<T> find(Filter filter, Class<T> clazz, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    <T> Iterable<T> find(Filter filter, Class<T> clazz, String[] sortFields, SortOrder[] sortOrders, String... withOptionalFields) throws IOException;

    <T> Iterable<T> find(Filter filter, Class<T> clazz, long offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    <T> Iterable<T> find(Filter filter, Class<T> clazz, long offset, int pageSize, String[] sortFields, SortOrder[] sortOrders, String... withOptionalFields) throws IOException;

    Iterable<DocType> find(String collectionName, Filter filter, String... withOptionalFields) throws IOException;

    Iterable<DocType> find(String collectionName, Filter filter, long offset, int pageSize, String... withOptionalFields) throws IOException;

    Iterable<DocType> find(String collectionName, Filter filter, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    Iterable<DocType> find(String collectionName, Filter filter, long offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, String... withOptionalFields) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, long offset, int pageSize, String... withOptionalFields) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, String[] sortFields, SortOrder[] sortOrders, String... withOptionalFields) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, long offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, long offset, int pageSize, String[] sortFields, SortOrder[] sortOrders, String... withOptionalFields) throws IOException;

    Iterable<DocType> findAll(String collectionName, String... withOptionalFields) throws IOException;

    Iterable<DocType> findAll(String collectionName, long offset, int pageSize, String... withOptionalFields) throws IOException;

    Iterable<DocType> findAll(String collectionName, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    Iterable<DocType> findAll(String collectionName, long offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException;

    <T> T injectOptionalFields(T object, String... optionalFields) throws IOException;

    DocType injectOptionalFields(String collectionName, DocType document, String... optionalFields) throws IOException;

    <T> Iterable<T> injectOptionalFields(Class<T> clazz, Iterable<T> objects, String... optionalFields) throws IOException;

    Iterable<DocType> injectOptionalFields(String collectionName, Iterable<DocType> documents, String... optionalFields) throws IOException;

    <P, C> Iterable<P> joinAllChildren(Class<C> childClass, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException;

    <P, C> Iterable<P> joinChildren(Class<C> childClass, Filter childFilter, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException;

    Iterable<DocType> joinAllChildren(String childCollection, Iterable<DocType> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException;

    Iterable<DocType> joinChildren(String childCollectionName, Filter childFilter, Iterable<DocType> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException;

    default <P, C> P fetchChild(final P parent, String matchingField, String targetField, Class<C> childClass, String... withOptionalChildFields) throws IOException {
        return fetchChild(parent, matchingField, matchingField, targetField, childClass, withOptionalChildFields);
    }

    default <P, C> P fetchChild(final P parent, String localField, String foreignField, String targetField, Class<C> childClass, String... withOptionalChildFields) throws IOException {
        try {
            Object matchingValue = getAllFieldValue(parent, localField);
            Field target = getAllField(parent.getClass(), targetField);

            List<C> targetChildren =
                    findStr(Filter.where(foreignField).eq(matchingValue), childClass, withOptionalChildFields)
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
                        collection = new LinkedBlockingDeque<>(targetChildren);
                    } else if (field.getType() == BlockingQueue.class) {
                        collection = new LinkedBlockingQueue<>(targetChildren);
                    } else if (field.getType() == Deque.class || field.getType() == Queue.class) {
                        collection = new ArrayDeque<>(targetChildren);
                    } else if (field.getType() == Set.class) {
                        collection = new HashSet<>(targetChildren);
                    } else if (field.getType() == SortedSet.class) {
                        collection = new TreeSet<>(targetChildren);
                    } else if (field.getType() == TransferQueue.class) {
                        collection = new LinkedTransferQueue<>(targetChildren);
                    } else {
                        collection = (Collection<C>) field.getType().getDeclaredConstructor().newInstance();
                        collection.addAll(targetChildren);
                    }
                }
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
                    findStr(Filter.where(foreignField).eq(matchingValue), childClass, withOptionalChildFields)
                            .toList();

            fetchChildren(parent, targetField, targetChildren);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return parent;
    }

    <T> boolean containsPrimaryKey(Object primaryKey, Class<T> clazz) throws IOException;

    <T> long count(Filter filter, Class<T> clazz) throws IOException;

    <T> long count(Filter filter, Class<T> clazz, long offset, int pageSize) throws IOException;

    <T> long countAll(Class<T> clazz) throws IOException;

    long count(String collectionName, Filter filter) throws IOException;

    long count(String collectionName, Filter filter, long offset, int pageSize) throws IOException;

    long countAll(String collectionName) throws IOException;

    <T> int remove(T object) throws IOException;

    <T> int removeByPrimaryKey(Object primaryKey, Class<T> clazz) throws IOException;

    <T> int removeAll(Iterable<T> objects) throws IOException;

    <T> int removeAll(Filter filter, Class<T> clazz) throws IOException;

    <T> boolean removeOne(Filter filter, Class<T> clazz) throws IOException;


    int remove(String collectionName, DocType document) throws IOException;

    int removeAll(String collectionName, Iterable<DocType> documents) throws IOException;

    int removeAll(String collectionName, Filter filter) throws IOException;

    //endregion

    <FilterType> FilterType getFilter(Filter filter);


    // region CRUD Streaming API
    default <T> Stream<T> findStr(Filter filter, Class<T> clazz, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(filter, clazz, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findStr(Filter filter, Class<T> clazz, long offset, int pageSize, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(filter, clazz, offset, pageSize, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findStr(Filter filter, Class<T> clazz, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(filter, clazz, sortField, sortOrder, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findStr(Filter filter, Class<T> clazz, String[] sortFields, SortOrder[] sortOrders, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(filter, clazz, sortFields, sortOrders, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findStr(Filter filter, Class<T> clazz, long offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(filter, clazz, offset, pageSize, sortField, sortOrder, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findStr(Filter filter, Class<T> clazz, long offset, int pageSize, String[] sortFields, SortOrder[] sortOrders, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(filter, clazz, offset, pageSize, sortFields, sortOrders, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findStr(String collectionName, Filter filter, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(collectionName, filter, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findStr(String collectionName, Filter filter, long offset, int pageSize, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(collectionName, filter, offset, pageSize, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findStr(String collectionName, Filter filter, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(collectionName, filter, sortField, sortOrder, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findStr(String collectionName, Filter filter, long offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(find(collectionName, filter, offset, pageSize, sortField, sortOrder, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findAllStr(Class<T> clazz, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(clazz, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findAllStr(Class<T> clazz, long offset, int pageSize, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(clazz, offset, pageSize, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findAllStr(Class<T> clazz, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(clazz, sortField, sortOrder, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findAllStr(Class<T> clazz, String[] sortFields, SortOrder[] sortOrders, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(clazz, sortFields, sortOrders, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findAllStr(Class<T> clazz, long offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(clazz, offset, pageSize, sortField, sortOrder, withOptionalFields).spliterator(), false);
    }

    default <T> Stream<T> findAllStr(Class<T> clazz, long offset, int pageSize, String[] sortFields, SortOrder[] sortOrders, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(clazz, offset, pageSize, sortFields, sortOrders, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findAllStr(String collectionName, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(collectionName, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findAllStr(String collectionName, long offset, int pageSize, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(collectionName, offset, pageSize, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findAllStr(String collectionName, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return StreamSupport.stream(findAll(collectionName, sortField, sortOrder, withOptionalFields).spliterator(), false);
    }

    default Stream<DocType> findAllStr(String collectionName, long offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
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

    default Stream<DocType> joinAllChildrenStr(String childCollectionName, Iterable<DocType> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return StreamSupport.stream(joinAllChildren(childCollectionName, parents, localField, foreignField, targetField, withOptionalChildFields).spliterator(), false);
    }

    default Stream<DocType> joinChildrenStr(String childCollectionName, Filter childFilter, Iterable<DocType> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return StreamSupport.stream(joinChildren(childCollectionName, childFilter, parents, localField, foreignField, targetField, withOptionalChildFields).spliterator(), false);
    }
    //endregion

    // region event support

    <T> long onInsert(Class<T> clazz, Consumer<T> listener, String... withOptionalFields) throws IOException;

    long onInsert(String collectionName, Consumer<DocType> listener, String... withOptionalFields) throws IOException;

    <T> long onUpdate(Class<T> clazz, Consumer<T> listener, String... withOptionalFields) throws IOException;

    long onUpdate(String collectionName, Consumer<DocType> listener, String... withOptionalFields) throws IOException;

    <T> long onRemove(Class<T> clazz, Consumer<T> listener, String... withOptionalFields) throws IOException;

    long onRemove(String collectionName, Consumer<DocType> listener, String... withOptionalFields) throws IOException;

    void unsubscribe(Class<?> clazz, long listenerId) throws IOException;

    void unsubscribe(String collectionName, long listenerId) throws IOException;

    // endregion

    //region transaction locks

    /**
     * Allow to perform multiple db action in a transaction without releasing the read lock.
     * Be careful read lock cannot be upgrade to write lock, so do never call a write operation
     * inside the read callable
     * @param transaction the READ operations to be performed as a transaction
     * @return the result produced by the transaction
     */
    <T> T read(Callable<T> transaction) throws IOException;

    /**
     * Allow to perform multiple db action in a transaction without releasing the write lock.
     * The write lock can be used for read operations too (reentrant ist supported).
     * So this method can be used for transactions that combine read and write operations.
     * @param transaction the READ operations to be performed as a transaction
     * @return the result produced by the transaction
     */
    <T> T write(Callable<T> transaction) throws IOException;

    default void read(IORunnable transaction) throws IOException{
        read(() -> {
            transaction.run();
            return null;
        });
    }

    default void write(IORunnable transaction) throws IOException{
        write(() -> {
            transaction.run();
            return null;
        });
    }
    //endregion

    Set<Class<?>> getAllRegisteredClasses();

}
