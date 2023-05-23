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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Iterables;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.Index;
import de.unijena.bioinf.storage.db.nosql.IndexType;
import de.unijena.bioinf.storage.db.nosql.*;
import org.apache.commons.lang3.tuple.Pair;
import org.dizitart.no2.*;
import org.dizitart.no2.filters.Filters;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NitriteDatabase implements Database<Document> {

    // Prevent illegal reflective access warnings
    static {
        if (!NitriteDatabase.class.getModule().isNamed()) {
            ClassLoader.class.getModule().addOpens(ClassLoader.class.getPackageName(), NitriteDatabase.class.getModule());
        }
    }

    protected Path file;

    // NITRITE
    private final Nitrite db;

    private final Map<String, NitriteCollection> collections = Collections.synchronizedMap(new HashMap<>());

    private final Map<Class<?>, ObjectRepository<?>> repositories = Collections.synchronizedMap(new HashMap<>());

    // LOCKS
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();
    private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
    private final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock stateWriteLock = stateLock.writeLock();
    private final ReentrantReadWriteLock.ReadLock stateReadLock = stateLock.readLock();

    // STATE
    private boolean isClosed = false;

    public NitriteDatabase(Path file, Metadata meta) throws IOException {
        this.file = file;
        this.db = initDB(file, meta);
        this.initCollections(meta.collectionIndices);
        this.initRepositories(meta.repoIndices, meta.idFields);
    }

    @SuppressWarnings("unchecked")
    private <T> void addSerializer(Metadata meta, SimpleModule module, Class<?> clazz, JsonSerializer<?> serializer) throws NoSuchFieldException {
        Class<T> c = (Class<T>) clazz;
        JsonSerializer<T> s = (JsonSerializer<T>) serializer;
        if (meta.idFields.containsKey(clazz)) {
            module.addSerializer(c, new NitriteIdMapperSerializer<>(c, meta.idFields.get(clazz), s));
        } else {
            module.addSerializer(c, s);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void addDeserializer(Metadata meta, SimpleModule module, Class<?> clazz, JsonDeserializer<?> deserializer) throws NoSuchFieldException {
        Class<T> c = (Class<T>) clazz;
        JsonDeserializer<T> d = (JsonDeserializer<T>) deserializer;
        if (meta.idFields.containsKey(clazz)) {
            module.addDeserializer(c, new NitriteIdMapperDeserializer<>(c, meta.idFields.get(clazz), d));
        } else {
            module.addDeserializer(c, d);
        }
    }

    private Nitrite initDB(Path file, Metadata meta) throws IOException {
        SimpleModule module = new SimpleModule("sirius-nitrite", Version.unknownVersion());
        try {
            for (Map.Entry<Class<?>, JsonSerializer<?>> entry : meta.serializers.entrySet()) {
                addSerializer(meta, module, entry.getKey(), entry.getValue());
            }
            for (Map.Entry<Class<?>, JsonDeserializer<?>> entry : meta.deserializers.entrySet()) {
                addDeserializer(meta, module, entry.getKey(), entry.getValue());
            }
        } catch (NoSuchFieldException e) {
            throw new IOException(e);
        }
        return Nitrite.builder().filePath(file.toFile()).registerModule(module).compressed().openOrCreate();
    }

    private void initCollections(Map<String, Index[]> collections) {
        for (String name : collections.keySet()) {
            NitriteCollection collection = this.db.getCollection(name);
            this.collections.put(name, collection);
            initIndex(collections.get(name), collection);
        }
    }

    private void initRepositories(Map<Class<?>, Index[]> repositories, Map<Class<?>, String> idFields) throws IOException {
        for (Class<?> clazz : repositories.keySet()) {
            ObjectRepository<?> repository = this.db.getRepository(clazz);
            this.repositories.put(clazz, repository);
            initIndex(repositories.get(clazz), repository);
            if (idFields.containsKey(clazz)) {
                initIdField(clazz, idFields.get(clazz), repository);
            }
        }
    }

    private void initIdField(Class<?> clazz, String idFieldName, ObjectRepository<?> repository) throws IOException {
        try {
            Field idField = clazz.getDeclaredField(idFieldName);
            Field fieldField = repository.getClass().getDeclaredField("idField");
            fieldField.setAccessible(true);
            fieldField.set(repository, idField);
            if (!repository.hasIndex(idFieldName)) {
                repository.createIndex(idFieldName, IndexOptions.indexOptions(org.dizitart.no2.IndexType.Unique));
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IOException(e);
        }
    }

    private <Repo extends PersistentCollection<?>> void initIndex(Index[] indices, Repo repository) {
        Collection<org.dizitart.no2.Index> repoIndices = repository.listIndices();
        List<org.dizitart.no2.Index> toDrop = new ArrayList<>();
        List<Index> toBuild = new ArrayList<>();
        for (Index index : indices) {
            found:
            {
                for (org.dizitart.no2.Index repoIndex : repoIndices) {
                    if (Objects.equals(repoIndex.getField(), index.getField())) {
                        org.dizitart.no2.IndexType repoType = repoIndex.getIndexType();
                        IndexType iType = index.getType();
                        if ((repoType == org.dizitart.no2.IndexType.Unique && iType != IndexType.UNIQUE) ||
                                (repoType == org.dizitart.no2.IndexType.NonUnique && iType != IndexType.NON_UNIQUE) ||
                                (repoType == org.dizitart.no2.IndexType.Fulltext && iType != IndexType.FULL_TEXT)) {
                            toDrop.add(repoIndex);
                            toBuild.add(index);
                        }
                        break found;
                    }
                }
                toBuild.add(index);
            }
        }

        for (org.dizitart.no2.Index index : toDrop) {
            repository.dropIndex(index.getField());
        }

        for (Index index : toBuild) {
            switch (index.getType()) {
                case UNIQUE:
                    repository.createIndex(index.getField(), IndexOptions.indexOptions(org.dizitart.no2.IndexType.Unique));
                    break;
                case NON_UNIQUE:
                    repository.createIndex(index.getField(), IndexOptions.indexOptions(org.dizitart.no2.IndexType.NonUnique));
                    break;
                case FULL_TEXT:
                    repository.createIndex(index.getField(), IndexOptions.indexOptions(org.dizitart.no2.IndexType.Fulltext));
                    break;
            }
        }
    }

    @Override
    public void close() {
        stateWriteLock.lock();
        try {
            this.isClosed = true;
            this.db.close();
        } finally {
            stateWriteLock.unlock();
        }
    }

    private <T> T callIfOpen(Callable<T> callable) throws IOException {
        stateReadLock.lock();
        if (this.isClosed) {
            throw new IOException("Nitrite database is closed!");
        }
        try {
            return callable.call();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            stateReadLock.unlock();
        }
    }

    private <T> T read(Callable<T> callable) throws IOException {
        return this.callIfOpen(() -> {
            readLock.lock();
            try {
                return callable.call();
            } finally {
                readLock.unlock();
            }
        });
    }

    private <T> T write(Callable<T> callable) throws IOException {
        return this.callIfOpen(() -> {
            writeLock.lock();
            try {
                return callable.call();
            } finally {
                writeLock.unlock();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectRepository<T> getRepository(Class<T> clazz) throws IOException {
        if (!this.repositories.containsKey(clazz)) {
            throw new IOException(clazz + " is not registered.");
        }
        return (ObjectRepository<T>) this.repositories.get(clazz);
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectRepository<T> getRepository(T object) throws IOException {
        if (!this.repositories.containsKey(object.getClass())) {
            throw new IOException(object.getClass() + " is not registered.");
        }
        return (ObjectRepository<T>) this.repositories.get(object.getClass());
    }

    @SuppressWarnings("unchecked")
    private <T> Pair<T[], ObjectRepository<T>> getRepository(Iterable<T> objects) throws IOException {
        Collection<T> collection = new ArrayList<>();
        Iterables.addAll(collection, objects);
        if (collection.isEmpty()) {
            return null;
        }
        T[] arr = (T[]) collection.toArray();
        Class<T> clazz = (Class<T>) arr[0].getClass();
        if (!this.repositories.containsKey(clazz)) {
            throw new IOException(clazz + " is not registered.");
        }
        return Pair.of(arr, (ObjectRepository<T>) this.repositories.get(clazz));
    }

    private NitriteCollection getCollection(String name) throws IOException {
        if (!this.collections.containsKey(name)) {
            throw new IOException(name + " is not registered.");
        }
        return this.collections.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> int insert(T object) throws IOException {
        return this.write(() -> {
            ObjectRepository<T> repo = this.getRepository(object);
            return repo.insert(object).getAffectedCount();
        });
    }

    @Override
    public <T> int insertAll(Iterable<T> objects) throws IOException {
        return this.write(() -> {
            Pair<T[], ObjectRepository<T>> pair = this.getRepository(objects);
            if (pair == null) {
                return 0;
            }
            return pair.getRight().insert(pair.getLeft()).getAffectedCount();
        });
    }

    @Override
    public int insert(String collectionName, Document document) throws IOException {
        return this.write(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            return collection.insert(document).getAffectedCount();
        });
    }

    @Override
    public int insertAll(String collectionName, Iterable<Document> documents) throws IOException {
        return this.write(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            Document[] docs = Iterables.toArray(documents, Document.class);
            return collection.insert(docs).getAffectedCount();
        });
    }

    @Override
    public <T> int upsert(T object) throws IOException {
        return this.write(() -> {
            ObjectRepository<T> repo = this.getRepository(object);
            return repo.update(object, true).getAffectedCount();
        });
    }

    @Override
    public <T> int upsertAll(Iterable<T> objects) throws IOException {
        return this.write(() -> {
            Pair<T[], ObjectRepository<T>> pair = this.getRepository(objects);
            if (pair == null) {
                return 0;
            }
            int count = 0;
            for (T o : pair.getLeft()) {
                count += pair.getRight().update(o, true).getAffectedCount();
            }
            return count;
        });
    }

    @Override
    public int upsert(String collectionName, Document document) throws IOException {
        return this.write(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            return collection.update(document, true).getAffectedCount();
        });
    }

    @Override
    public int upsertAll(String collectionName, Iterable<Document> documents) throws IOException {
        return this.write(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            int count = 0;
            for (Document doc : documents) {
                count += collection.update(doc, true).getAffectedCount();
            }
            return count;
        });
    }

    @Override
    public <T> T getById(long id, Class<T> clazz) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            return repo.getById(NitriteId.createId(id));
        });
    }

    @Override
    public Document getById(String collectionName, long id) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            return collection.getById(NitriteId.createId(id));
        });
    }

    @Override
    public <T> Iterable<T> find(Filter filter, Class<T> clazz) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            ObjectFilter of = getObjectFilter(filter);
            return repo.find(of);
        });
    }

    @Override
    public <T> Iterable<T> find(Filter filter, Class<T> clazz, int offset, int pageSize) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            ObjectFilter of = getObjectFilter(filter);
            return repo.find(of, FindOptions.limit(offset, pageSize));
        });
    }

    @Override
    public <T> Iterable<T> find(Filter filter, Class<T> clazz, String sortField, SortOrder sortOrder) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            ObjectFilter of = getObjectFilter(filter);
            return repo.find(of, FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending));
        });
    }

    @Override
    public <T> Iterable<T> find(Filter filter, Class<T> clazz, int offset, int pageSize, String sortField, SortOrder sortOrder) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            ObjectFilter of = getObjectFilter(filter);
            return repo.find(of, FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending).thenLimit(offset, pageSize));
        });
    }

    @Override
    public Iterable<Document> find(String collectionName, Filter filter) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            org.dizitart.no2.Filter f = getFilter(filter);
            return collection.find(f);
        });
    }

    @Override
    public Iterable<Document> find(String collectionName, Filter filter, int offset, int pageSize) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            org.dizitart.no2.Filter f = getFilter(filter);
            return collection.find(f, FindOptions.limit(offset, pageSize));
        });
    }

    @Override
    public Iterable<Document> find(String collectionName, Filter filter, String sortField, SortOrder sortOrder) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            org.dizitart.no2.Filter f = getFilter(filter);
            return collection.find(f, FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending));
        });
    }

    @Override
    public Iterable<Document> find(String collectionName, Filter filter, int offset, int pageSize, String sortField, SortOrder sortOrder) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            org.dizitart.no2.Filter f = getFilter(filter);
            return collection.find(f, FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending).thenLimit(offset, pageSize));
        });
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> clazz) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            return repo.find();
        });
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> clazz, int offset, int pageSize) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            return repo.find(FindOptions.limit(offset, pageSize));
        });
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> clazz, String sortField, SortOrder sortOrder) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            return repo.find(
                    FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending)
            );
        });
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> clazz, int offset, int pageSize, String sortField, SortOrder sortOrder) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            return repo.find(
                    FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending).thenLimit(offset, pageSize)
            );
        });
    }

    @Override
    public Iterable<Document> findAll(String collectionName) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            return collection.find();
        });
    }

    @Override
    public Iterable<Document> findAll(String collectionName, int offset, int pageSize) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            return collection.find(FindOptions.limit(offset, pageSize));
        });
    }

    @Override
    public Iterable<Document> findAll(String collectionName, String sortField, SortOrder sortOrder) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            return collection.find(
                    FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending)
            );
        });
    }

    @Override
    public Iterable<Document> findAll(String collectionName, int offset, int pageSize, String sortField, SortOrder sortOrder) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            return collection.find(
                    FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending).thenLimit(offset, pageSize)
            );
        });
    }

    public <T, P, C> Iterable<T> joinAllChildren(Class<T> clazz, Class<P> parentClass, Class<C> childClass, Iterable<P> parents, String foreignField, String targetField) {
        return NitriteJoinedIterable.newInstance(clazz, parentClass, childClass, parents, foreignField, targetField, this);
    }

    public <T, P, C> Iterable<T> joinChildren(Class<T> clazz, Class<P> parentClass, Class<C> childClass, Filter childFilter, Iterable<P> parents, String foreignField, String targetField) throws IOException {
        return NitriteFilteredJoinedIterable.newInstance(clazz, parentClass, childClass, childFilter, parents, foreignField, targetField, this);
    }

    @Override
    public Iterable<Document> joinAllChildren(String parentCollectionName, String childCollectionName, Iterable<Document> parents, String foreignField, String targetField) {
        return NitriteJoinedDocumentIterable.newInstance(parentCollectionName, childCollectionName, parents, foreignField, targetField, this);
    }

    @Override
    public Iterable<Document> joinChildren(String parentCollectionName, String childCollectionName, Filter childFilter, Iterable<Document> parents, String foreignField, String targetField) throws IOException {
        return NitriteFilteredJoinedDocumentIterable.newInstance(parentCollectionName, childCollectionName, childFilter, parents, foreignField, targetField, this);
    }

    @Override
    public <T> int count(Filter filter, Class<T> clazz) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            ObjectFilter of = getObjectFilter(filter);
            return repo.find(of).size();
        });
    }

    @Override
    public <T> int count(Filter filter, Class<T> clazz, int offset, int pageSize) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            ObjectFilter of = getObjectFilter(filter);
            return repo.find(of, FindOptions.limit(offset, pageSize)).size();
        });
    }

    @Override
    public <T> int countAll(Class<T> clazz) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            return repo.find().totalCount();
        });
    }

    @Override
    public int count(String collectionName, Filter filter) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            org.dizitart.no2.Filter f = getFilter(filter);
            return collection.find(f).size();
        });
    }

    @Override
    public int count(String collectionName, Filter filter, int offset, int pageSize) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            org.dizitart.no2.Filter f = getFilter(filter);
            return collection.find(f, FindOptions.limit(offset, pageSize)).size();
        });
    }

    @Override
    public int countAll(String collectionName) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            return collection.find().totalCount();
        });
    }

    @Override
    public <T> int remove(T object) throws IOException {
        return this.write(() -> {
            ObjectRepository<T> repo = this.getRepository(object);
            return repo.remove(object).getAffectedCount();
        });
    }

    @Override
    public <T> int removeAll(Iterable<T> objects) throws IOException {
        return this.write(() -> {
            Pair<T[], ObjectRepository<T>> pair = this.getRepository(objects);
            if (pair == null) {
                return 0;
            }
            int count = 0;
            for (T o : pair.getLeft()) {
                count += pair.getRight().remove(o).getAffectedCount();
            }
            return count;
        });
    }

    @Override
    public <T> int removeAll(Filter filter, Class<T> clazz) throws IOException {
        return this.write(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            ObjectFilter of = getObjectFilter(filter);
            return repo.remove(of).getAffectedCount();
        });
    }

    @Override
    public int remove(String collectionName, Document document) throws IOException {
        return this.write(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            return collection.remove(document).getAffectedCount();
        });
    }

    @Override
    public int removeAll(String collectionName, Iterable<Document> documents) throws IOException {
        return this.write(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            int count = 0;
            for (Document doc : documents) {
                count += collection.remove(doc).getAffectedCount();
            }
            return count;
        });
    }

    @Override
    public int removeAll(String collectionName, Filter filter) throws IOException {
        return this.write(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            org.dizitart.no2.Filter f = getFilter(filter);
            return collection.remove(f).getAffectedCount();
        });
    }

    private ObjectFilter[] getAllObjectFilterChildren(Deque<Filter.FilterElement> filterChain) {
        List<ObjectFilter> children = new ArrayList<>();
        while (!filterChain.isEmpty()) {
            ObjectFilter next = getObjectFilter(filterChain);
            if (next == ObjectFilters.ALL) {
                break;
            }
            children.add(next);
        }
        ObjectFilter[] arr = new ObjectFilter[children.size()];
        return children.toArray(arr);
    }

    private ObjectFilter getObjectFilter(Deque<Filter.FilterElement> filterChain) {
        if (filterChain.isEmpty()) {
            return ObjectFilters.ALL;
        }
        Filter.FilterElement element = filterChain.pop();
        switch (element.filterType) {
            case AND:
                return ObjectFilters.and(getAllObjectFilterChildren(filterChain));
            case OR:
                return ObjectFilters.or(getAllObjectFilterChildren(filterChain));
            case NOT:
                return ObjectFilters.not(getObjectFilter(filterChain));
            case EQ:
                return ObjectFilters.eq(
                        ((Filter.FieldFilterElement) element).field,
                        ((Filter.FieldFilterElement) element).values[0]
                );
            case GT:
                return ObjectFilters.gt(
                        ((Filter.FieldFilterElement) element).field,
                        ((Filter.FieldFilterElement) element).values[0]
                );
            case GTE:
                return ObjectFilters.gte(
                        ((Filter.FieldFilterElement) element).field,
                        ((Filter.FieldFilterElement) element).values[0]
                );
            case LT:
                return ObjectFilters.lt(
                        ((Filter.FieldFilterElement) element).field,
                        ((Filter.FieldFilterElement) element).values[0]
                );
            case LTE:
                return ObjectFilters.lte(
                        ((Filter.FieldFilterElement) element).field,
                        ((Filter.FieldFilterElement) element).values[0]
                );
            case TEXT:
                return ObjectFilters.text(
                        ((Filter.FieldFilterElement) element).field,
                        (String) ((Filter.FieldFilterElement) element).values[0]
                );
            case REGEX:
                return ObjectFilters.regex(
                        ((Filter.FieldFilterElement) element).field,
                        (String) ((Filter.FieldFilterElement) element).values[0]
                );
            case IN:
                return ObjectFilters.in(
                        ((Filter.FieldFilterElement) element).field,
                        ((Filter.FieldFilterElement) element).values
                );
            case NOT_IN:
                return ObjectFilters.notIn(
                        ((Filter.FieldFilterElement) element).field,
                        ((Filter.FieldFilterElement) element).values
                );
            case ELEM_MATCH:
                return ObjectFilters.elemMatch(
                        ((Filter.FieldFilterElement) element).field,
                        getObjectFilter(filterChain)
                );
        }
        return ObjectFilters.ALL;
    }

    private org.dizitart.no2.Filter[] getAllFilterChildren(Deque<Filter.FilterElement> filterChain) {
        List<org.dizitart.no2.Filter> children = new ArrayList<>();
        while (!filterChain.isEmpty()) {
            org.dizitart.no2.Filter next = getFilter(filterChain);
            if (next == Filters.ALL) {
                break;
            }
            children.add(next);
        }
        org.dizitart.no2.Filter[] arr = new org.dizitart.no2.Filter[children.size()];
        return children.toArray(arr);
    }

    private org.dizitart.no2.Filter getFilter(Deque<Filter.FilterElement> filterChain) {
        if (filterChain.isEmpty()) {
            return Filters.ALL;
        }
        Filter.FilterElement element = filterChain.pop();
        switch (element.filterType) {
            case AND:
                return Filters.and(getAllFilterChildren(filterChain));
            case OR:
                return Filters.or(getAllFilterChildren(filterChain));
            case NOT:
                return Filters.not(getFilter(filterChain));
            case EQ:
                return Filters.eq(
                        ((Filter.FieldFilterElement) element).field,
                        ((Filter.FieldFilterElement) element).values[0]
                );
            case GT:
                return Filters.gt(
                        ((Filter.FieldFilterElement) element).field,
                        ((Filter.FieldFilterElement) element).values[0]
                );
            case GTE:
                return Filters.gte(
                        ((Filter.FieldFilterElement) element).field,
                        ((Filter.FieldFilterElement) element).values[0]
                );
            case LT:
                return Filters.lt(
                        ((Filter.FieldFilterElement) element).field,
                        ((Filter.FieldFilterElement) element).values[0]
                );
            case LTE:
                return Filters.lte(
                        ((Filter.FieldFilterElement) element).field,
                        ((Filter.FieldFilterElement) element).values[0]
                );
            case TEXT:
                return Filters.text(
                        ((Filter.FieldFilterElement) element).field,
                        (String) ((Filter.FieldFilterElement) element).values[0]
                );
            case REGEX:
                return Filters.regex(
                        ((Filter.FieldFilterElement) element).field,
                        (String) ((Filter.FieldFilterElement) element).values[0]
                );
            case IN:
                return Filters.in(
                        ((Filter.FieldFilterElement) element).field,
                        ((Filter.FieldFilterElement) element).values
                );
            case NOT_IN:
                return Filters.notIn(
                        ((Filter.FieldFilterElement) element).field,
                        ((Filter.FieldFilterElement) element).values
                );
            case ELEM_MATCH:
                return Filters.elemMatch(
                        ((Filter.FieldFilterElement) element).field,
                        getFilter(filterChain)
                );
        }
        return Filters.ALL;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ObjectFilter getObjectFilter(Filter filter) {
        return getObjectFilter(new ArrayDeque<>(filter.filterChain));
    }

    @Override
    @SuppressWarnings("unchecked")
    public org.dizitart.no2.Filter getFilter(Filter filter) {
        return getFilter(new ArrayDeque<>(filter.filterChain));
    }
}
