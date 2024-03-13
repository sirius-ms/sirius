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
import de.unijena.bioinf.storage.db.nosql.utils.PKSuppliers;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.dizitart.no2.*;
import org.dizitart.no2.filters.Filters;
import org.dizitart.no2.mapper.JacksonMapper;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.dizitart.no2.util.ObjectUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import static org.dizitart.no2.exceptions.ErrorMessage.*;
import static org.dizitart.no2.objects.filters.ObjectFilters.eq;

public class NitriteDatabase implements Database<Document> {
    protected Path file;

    // NITRITE
    private final Nitrite db;

    private final JacksonMapper nitriteMapper;

    private final Map<Class<?>, ObjectRepository<?>> repositories = Collections.synchronizedMap(new HashMap<>());

    private final Map<Class<?>, Set<String>> optionalRepoFields = Collections.synchronizedMap(new HashMap<>());

    private final Map<Class<?>, Field> primaryKeyFields = Collections.synchronizedMap(new HashMap<>());

    private final Map<Class<?>, Supplier<?>> primaryKeySuppliers = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, Set<String>> optionalCollectionFields = Collections.synchronizedMap(new HashMap<>());

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
        this.initCollections(meta);
        this.initRepositories(meta);
        this.initOptionalFields(meta);
        try {
            Field cField = Nitrite.class.getDeclaredField("context");
            cField.setAccessible(true);
            NitriteContext context = (NitriteContext) cField.get(this.db);
            this.nitriteMapper = (JacksonMapper) context.getNitriteMapper();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IOException(e);
        }
    }

    public JacksonMapper getJacksonMapper() {
        return this.nitriteMapper;
    }

    private Nitrite initDB(Path file, Metadata meta) {
        SimpleModule module = new SimpleModule("sirius-nitrite", Version.unknownVersion());
        for (Map.Entry<Class<?>, JsonSerializer<?>> entry : meta.serializers.entrySet()) {
            addSerializer(module, entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Class<?>, JsonDeserializer<?>> entry : meta.deserializers.entrySet()) {
            addDeserializer(module, entry.getKey(), entry.getValue());
        }
        return Nitrite.builder().filePath(file.toFile()).registerModule(module).compressed().openOrCreate();
    }

    private void initCollections(Metadata meta) {
        for (String name : meta.collectionIndices.keySet()) {
            NitriteCollection collection = this.db.getCollection(name);
            initIndex(meta.collectionIndices.get(name), collection);
        }
    }

    private void initRepositories(Metadata meta) throws IOException {
        for (Class<?> clazz : meta.repoIndices.keySet()) {
            ObjectRepository<?> repository = this.db.getRepository(clazz);
            this.repositories.put(clazz, repository);
            initIndex(meta.repoIndices.get(clazz), repository);
            initPrimaryKey(clazz, meta.pkFields.get(clazz), meta.pkSuppliers.get(clazz), repository);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void addSerializer(SimpleModule module, Class<?> clazz, JsonSerializer<?> serializer) {
        Class<T> c = (Class<T>) clazz;
        JsonSerializer<T> s = (JsonSerializer<T>) serializer;
        module.addSerializer(c, s);
    }

    @SuppressWarnings("unchecked")
    private <T> void addDeserializer(SimpleModule module, Class<?> clazz, JsonDeserializer<?> deserializer) {
        Class<T> c = (Class<T>) clazz;
        JsonDeserializer<T> d = (JsonDeserializer<T>) deserializer;
        module.addDeserializer(c, d);
    }

    private void initOptionalFields(Metadata meta) {
        meta.optionalRepoFields.forEach((clazz, fields) -> optionalRepoFields.put(clazz, new HashSet<>(Arrays.asList(fields))));
        meta.optionalCollectionFields.forEach((collection, fields) -> optionalCollectionFields.put(collection, new HashSet<>(Arrays.asList(fields))));
    }

    private void initPrimaryKey(Class<?> clazz, Field pkField, @Nullable Supplier<?> pkSupplier, ObjectRepository<?> repository) throws IOException {
        String pkName = pkField.getName();
        this.primaryKeyFields.put(clazz, pkField);
        Class<?> fieldType = pkField.getType();
        if (pkSupplier != null) {
            try {
                Class<?> supplierClass = pkSupplier.getClass();
                Method getMethod = supplierClass.getMethod("get");
                if (supplierClass.isSynthetic() && !getMethod.isSynthetic()) {
                    // If the supplier is a lambda, we are unable to check if the return type is correct :(
                    this.primaryKeySuppliers.put(clazz, pkSupplier);
                } else {
                    Class<?> returnType = getMethod.getReturnType();
                    if (fieldType.equals(returnType) || ClassUtils.getAllSuperclasses(returnType).contains(fieldType)) {
                        this.primaryKeySuppliers.put(clazz, pkSupplier);
                    } else {
                        throw new IOException("Invalid primary key supplier! " +
                                clazz + "." + pkField.getName() + " has type " + pkField.getType() +
                                " but the primary key supplier returns " + returnType);
                    }
                }
            } catch (NoSuchMethodException e) {
                throw new IOException(e);
            }
        } else {
            if (fieldType.equals(String.class)) {
                this.primaryKeySuppliers.put(clazz, PKSuppliers.getStringKey());
            } else if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
                this.primaryKeySuppliers.put(clazz, PKSuppliers.getLongKey());
            } else if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
                this.primaryKeySuppliers.put(clazz, PKSuppliers.getDoubleKey());
            } else if (fieldType.equals(BigInteger.class)) {
                this.primaryKeySuppliers.put(clazz, PKSuppliers.getBigIntKey());
            } else if (fieldType.equals(BigDecimal.class)) {
                this.primaryKeySuppliers.put(clazz, PKSuppliers.getBigDecimalKey());
            }

        }
        if (!repository.hasIndex(pkName)) {
            repository.createIndex(pkName, IndexOptions.indexOptions(org.dizitart.no2.IndexType.Unique));
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
    private <T> Triple<T[], ObjectRepository<T>, Class<T>> getRepository(Iterable<T> objects) throws IOException {
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
        return Triple.of(arr, (ObjectRepository<T>) this.repositories.get(clazz), clazz);
    }

    private NitriteCollection getCollection(String name) throws IOException {
        //collection of repos are created on demand, so we have to check both
        if (!db.hasCollection(name) && !db.listRepositories().contains(name)) {
            throw new IOException(name + " is not registered.");
        }

        return db.getCollection(name);
    }

    private <T> Iterable<T> maybeProject(Class<T> clazz, @Nullable Filter filter, @Nullable FindOptions findOptions, String[] withOptionalFields) throws IOException {
        if (optionalRepoFields.containsKey(clazz)) {
            Set<String> omittedFields = new HashSet<>(optionalRepoFields.get(clazz));
            omittedFields.removeAll(new HashSet<>(Arrays.asList(withOptionalFields)));
            if (!omittedFields.isEmpty()) {
                Iterable<Document> result = doFindDocument(getRepository(clazz).getDocumentCollection(), filter, findOptions);
                return new ProjectingIterable<>(clazz, result, omittedFields, nitriteMapper);
            }
        }
        return doFind(clazz, filter, findOptions);
    }

    private Iterable<Document> maybeProjectDocuments(String collectionName, @Nullable Filter filter, @Nullable FindOptions findOptions, String[] withOptionalFields) throws IOException {
        Iterable<Document> result = doFindDocument(getCollection(collectionName), filter, findOptions);
        if (optionalCollectionFields.containsKey(collectionName)) {
            Set<String> omittedFields = new HashSet<>(optionalCollectionFields.get(collectionName));
            omittedFields.removeAll(new HashSet<>(Arrays.asList(withOptionalFields)));
            if (!omittedFields.isEmpty()) {
                return new ProjectingDocumentIterable(result, omittedFields);
            }
        }
        return result;
    }

    private <T> Iterable<T> doFind(Class<T> clazz, @Nullable Filter filter, @Nullable FindOptions findOptions) throws IOException {
        if (filter != null && findOptions != null) {
            return getRepository(clazz).find(getObjectFilter(filter), findOptions);
        } else if (filter != null) {
            return getRepository(clazz).find(getObjectFilter(filter));
        } else if (findOptions != null) {
            return getRepository(clazz).find(findOptions);
        } else {
            return getRepository(clazz).find();
        }
    }

    private Iterable<Document> doFindDocument(NitriteCollection collection, @Nullable Filter filter, @Nullable FindOptions findOptions) throws IOException {
        if (filter != null && findOptions != null) {
            return collection.find(getFilter(filter), findOptions);
        } else if (filter != null) {
            return collection.find(getFilter(filter));
        } else if (findOptions != null) {
            return collection.find(findOptions);
        } else {
            return collection.find();
        }
    }

    @Override
    public Path location() {
        return file;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> int insert(T object) throws IOException {
        return this.write(() -> {
            ObjectRepository<T> repo = this.getRepository(object);
            Class<T> clazz = (Class<T>) object.getClass();
            Field pkField = primaryKeyFields.get(clazz);
            if (getPrimaryKeyValue(object, pkField).isEmpty()) {
                if (!this.primaryKeySuppliers.containsKey(clazz)) {
                    throw new IOException(ID_CAN_NOT_BE_NULL.getMessage());
                }
                Object pk = this.primaryKeySuppliers.get(clazz).get();
                pkField.set(object, pk);
            }
            return repo.insert(object).getAffectedCount();
        });
    }

    @Override
    public <T> int insertAll(Iterable<T> objects) throws IOException {
        return this.write(() -> {
            Triple<T[], ObjectRepository<T>, Class<T>> triple = this.getRepository(objects);
            if (triple == null) {
                return 0;
            }
            Field pkField = primaryKeyFields.get(triple.getRight());
            Supplier<?> primaryKeySupplier = this.primaryKeySuppliers.get(triple.getRight());
            for (T object : triple.getLeft()) {
                if (getPrimaryKeyValue(object, pkField).isEmpty()) {
                    if (primaryKeySupplier == null) {
                        throw new IOException(ID_CAN_NOT_BE_NULL.getMessage());
                    }
                    pkField.set(object, primaryKeySupplier.get());
                }
            }
            return triple.getMiddle().insert(triple.getLeft()).getAffectedCount();
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
            Pair<Object, ObjectFilter> filter = createUniqueFilter(object, primaryKeyFields.get(object.getClass()));
            if (filter.getLeft() != null) {
                return repo.update(filter.getRight(), object, true).getAffectedCount();
            } else {
                return insert(object);
            }
        });
    }

    @Override
    public <T> int upsertAll(Iterable<T> objects) throws IOException {
        return this.write(() -> {
            Triple<T[], ObjectRepository<T>, Class<T>> triple = this.getRepository(objects);
            if (triple == null) {
                return 0;
            }
            int count = 0;
            List<T> toInsert = new ArrayList<>();
            for (T o : triple.getLeft()) {
                Pair<Object, ObjectFilter> filter = createUniqueFilter(o, primaryKeyFields.get(o.getClass()));
                if (filter.getLeft() != null) {
                    count += triple.getMiddle().update(filter.getRight(), o, true).getAffectedCount();
                } else {
                    toInsert.add(o);
                }
            }
            if (!toInsert.isEmpty()) {
                count += insertAll(toInsert);
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
    public <T> Optional<T> getByPrimaryKey(Object primaryKey, Class<T> clazz, String... withOptionalFields) throws IOException {
        return this.read(() -> {
            List<T> results = Lists.newArrayList(maybeProject(clazz, Filter.build().eq(this.primaryKeyFields.get(clazz).getName(), primaryKey), null, withOptionalFields).iterator());
            if (results.isEmpty()) {
                return Optional.empty();
            } else if (results.size() > 1) {
                throw new IOException("Primary key is not unique!");
            }
            return Optional.of(results.get(0));
        });
    }

    public Optional<Document> getByNitriteId(String collectionName, NitriteId id, String... withOptionalFields) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            Document document = collection.getById(id);
            if (document == null) {
                return Optional.empty();
            }
            if (optionalCollectionFields.containsKey(collectionName)) {
                Set<String> omittedFields = new HashSet<>(optionalCollectionFields.get(collectionName));
                omittedFields.removeAll(new HashSet<>(Arrays.asList(withOptionalFields)));
                if (!omittedFields.isEmpty()) {
                    return Optional.of(ProjectingDocumentIterable.project(document, omittedFields));
                }
            }
            return Optional.of(document);
        });
    }

    @Override
    public <T> Iterable<T> find(Filter filter, Class<T> clazz, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, filter, null, withOptionalFields));
    }

    @Override
    public <T> Iterable<T> find(Filter filter, Class<T> clazz, int offset, int pageSize, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, filter, FindOptions.limit(offset, pageSize), withOptionalFields));
    }

    @Override
    public <T> Iterable<T> find(Filter filter, Class<T> clazz, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, filter, FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending), withOptionalFields));
    }

    @Override
    public <T> Iterable<T> find(Filter filter, Class<T> clazz, int offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, filter, FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending).thenLimit(offset, pageSize), withOptionalFields));
    }

    @Override
    public Iterable<Document> find(String collectionName, Filter filter, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, filter, null, withOptionalFields));
    }

    @Override
    public Iterable<Document> find(String collectionName, Filter filter, int offset, int pageSize, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, filter, FindOptions.limit(offset, pageSize), withOptionalFields));
    }

    @Override
    public Iterable<Document> find(String collectionName, Filter filter, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, filter, FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending), withOptionalFields));
    }

    @Override
    public Iterable<Document> find(String collectionName, Filter filter, int offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, filter, FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending).thenLimit(offset, pageSize), withOptionalFields));
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> clazz, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, null, null, withOptionalFields));
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> clazz, int offset, int pageSize, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, null, FindOptions.limit(offset, pageSize), withOptionalFields));
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> clazz, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, null, FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending), withOptionalFields));
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> clazz, int offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, null, FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending).thenLimit(offset, pageSize), withOptionalFields));
    }

    @Override
    public Iterable<Document> findAll(String collectionName, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, null, null, withOptionalFields));
    }

    @Override
    public Iterable<Document> findAll(String collectionName, int offset, int pageSize, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, null, FindOptions.limit(offset, pageSize), withOptionalFields));
    }

    @Override
    public Iterable<Document> findAll(String collectionName, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, null, FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending), withOptionalFields));
    }

    @Override
    public Iterable<Document> findAll(String collectionName, int offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, null, FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending).thenLimit(offset, pageSize), withOptionalFields));
    }

    @Override
    public <T> T injectOptionalFields(T object, String... optionalFields) throws IOException {
        NitriteCollection collection = getRepository(object).getDocumentCollection();
        return InjectingIterable.inject(object, new HashSet<>(Arrays.asList(optionalFields)), collection, primaryKeyFields.get(object.getClass()), nitriteMapper);
    }

    @Override
    public Document injectOptionalFields(String collectionName, Document document, String... optionalFields) throws IOException {
        return InjectingDocumentIterable.inject(document, new HashSet<>(Arrays.asList(optionalFields)), getCollection(collectionName));
    }

    @Override
    public <T> Iterable<T> injectOptionalFields(Class<T> clazz, Iterable<T> objects, String... optionalFields) throws IOException {
        if (objects instanceof ProjectingIterable<T>) {
            ((ProjectingIterable<T>) objects).withOptionalFields(new HashSet<>(Arrays.asList(optionalFields)));
            return objects;
        } else {
            return new InjectingIterable<>(objects, new HashSet<>(Arrays.asList(optionalFields)), getRepository(clazz), primaryKeyFields.get(clazz), nitriteMapper);
        }
    }

    @Override
    public Iterable<Document> injectOptionalFields(String collectionName, Iterable<Document> documents, String... optionalFields) throws IOException {
        if (documents instanceof ProjectingDocumentIterable) {
            ((ProjectingDocumentIterable) documents).withOptionalFields(new HashSet<>(Arrays.asList(optionalFields)));
            return documents;
        } else {
            return new InjectingDocumentIterable(documents, new HashSet<>(Arrays.asList(optionalFields)), getCollection(collectionName));
        }
    }

    @Override
    public <P, C> Iterable<P> joinAllChildren(Class<C> childClass, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return new JoinedReflectionIterable<>(
                childClass,
                parents,
                (localObject) -> {
                    try {
                        return maybeProjectDocuments(childClass.getName(), Filter.build().eq(foreignField, localObject), null, withOptionalChildFields);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                localField,
                targetField,
                nitriteMapper
        );
    }

    @Override
    public <P, C> Iterable<P> joinChildren(Class<C> childClass, Filter childFilter, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return new JoinedReflectionIterable<>(
                childClass,
                parents,
                (localObject) -> {
                    try {
                        Filter cFilter = new Filter().and().eq(foreignField, localObject);
                        cFilter.filterChain.addAll(childFilter.filterChain);
                        return maybeProjectDocuments(childClass.getName(), cFilter, null, withOptionalChildFields);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                localField,
                targetField,
                nitriteMapper
        );
    }

    @Override
    public <T, P, C> Iterable<T> joinAllChildren(Class<T> targetClass, Class<C> childClass, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        if (parents instanceof org.dizitart.no2.objects.Cursor<P> pCursor) {
            return new JoinedIterable<>(
                    targetClass,
                    pCursor,
                    (localObject) -> {
                        try {
                            return maybeProjectDocuments(childClass.getName(), Filter.build().eq(foreignField, localObject), null, withOptionalChildFields);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    localField,
                    targetField,
                    nitriteMapper
            );
        } else {
            throw new IllegalArgumentException("parents must be a org.dizitart.no2.objects.Cursor!");
        }
    }

    @Override
    public <T, P, C> Iterable<T> joinChildren(Class<T> targetClass, Class<C> childClass, Filter childFilter, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        if (parents instanceof org.dizitart.no2.objects.Cursor<P> pCursor) {
            return new JoinedIterable<>(
                    targetClass,
                    pCursor,
                    (localObject) -> {
                        try {
                            Filter cFilter = new Filter().and().eq(foreignField, localObject);
                            cFilter.filterChain.addAll(childFilter.filterChain);
                            return maybeProjectDocuments(childClass.getName(), cFilter, null, withOptionalChildFields);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    localField,
                    targetField,
                    nitriteMapper
            );
        } else {
            throw new IllegalArgumentException("parents must be a org.dizitart.no2.objects.Cursor!");
        }
    }

    @Override
    public Iterable<Document> joinAllChildren(String childCollectionName, Iterable<Document> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return () -> new JoinedDocumentIterator(
                parents,
                (localObject) -> {
                    try {
                        return find(childCollectionName, new Filter().eq(foreignField, localObject), withOptionalChildFields);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                localField,
                targetField
        );
    }

    @Override
    public Iterable<Document> joinChildren(String childCollectionName, Filter childFilter, Iterable<Document> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return () -> new JoinedDocumentIterator(
                parents,
                (localObject) -> {
                    try {
                        Filter cFilter = new Filter().and().eq(foreignField, localObject);
                        cFilter.filterChain.addAll(childFilter.filterChain);
                        return find(childCollectionName, cFilter, withOptionalChildFields);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                localField,
                targetField
        );
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
            ObjectFilter filter = ObjectUtils.createUniqueFilter(object, primaryKeyFields.get(object.getClass()));
            return repo.remove(filter).getAffectedCount();
        });
    }

    @Override
    public <T> int removeAll(Iterable<T> objects) throws IOException {
        return this.write(() -> {
            Triple<T[], ObjectRepository<T>, Class<T>> triple = this.getRepository(objects);
            if (triple == null) {
                return 0;
            }
            int count = 0;
            for (T o : triple.getLeft()) {
                ObjectFilter filter = ObjectUtils.createUniqueFilter(o, primaryKeyFields.get(o.getClass()));
                count += triple.getMiddle().remove(filter).getAffectedCount();
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
        return switch (element.filterType) {
            case AND -> ObjectFilters.and(getAllObjectFilterChildren(filterChain));
            case OR -> ObjectFilters.or(getAllObjectFilterChildren(filterChain));
            case NOT -> ObjectFilters.not(getObjectFilter(filterChain));
            case EQ -> ObjectFilters.eq(
                    ((Filter.FieldFilterElement) element).field,
                    ((Filter.FieldFilterElement) element).values[0]
            );
            case GT -> ObjectFilters.gt(
                    ((Filter.FieldFilterElement) element).field,
                    ((Filter.FieldFilterElement) element).values[0]
            );
            case GTE -> ObjectFilters.gte(
                    ((Filter.FieldFilterElement) element).field,
                    ((Filter.FieldFilterElement) element).values[0]
            );
            case LT -> ObjectFilters.lt(
                    ((Filter.FieldFilterElement) element).field,
                    ((Filter.FieldFilterElement) element).values[0]
            );
            case LTE -> ObjectFilters.lte(
                    ((Filter.FieldFilterElement) element).field,
                    ((Filter.FieldFilterElement) element).values[0]
            );
            case TEXT -> ObjectFilters.text(
                    ((Filter.FieldFilterElement) element).field,
                    (String) ((Filter.FieldFilterElement) element).values[0]
            );
            case REGEX -> ObjectFilters.regex(
                    ((Filter.FieldFilterElement) element).field,
                    (String) ((Filter.FieldFilterElement) element).values[0]
            );
            case IN -> ObjectFilters.in(
                    ((Filter.FieldFilterElement) element).field,
                    ((Filter.FieldFilterElement) element).values
            );
            case NOT_IN -> ObjectFilters.notIn(
                    ((Filter.FieldFilterElement) element).field,
                    ((Filter.FieldFilterElement) element).values
            );
            case ELEM_MATCH -> ObjectFilters.elemMatch(
                    ((Filter.FieldFilterElement) element).field,
                    getObjectFilter(filterChain)
            );
            default -> ObjectFilters.ALL;
        };
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
        return switch (element.filterType) {
            case AND -> Filters.and(getAllFilterChildren(filterChain));
            case OR -> Filters.or(getAllFilterChildren(filterChain));
            case NOT -> Filters.not(getFilter(filterChain));
            case EQ -> Filters.eq(
                    ((Filter.FieldFilterElement) element).field,
                    ((Filter.FieldFilterElement) element).values[0]
            );
            case GT -> Filters.gt(
                    ((Filter.FieldFilterElement) element).field,
                    ((Filter.FieldFilterElement) element).values[0]
            );
            case GTE -> Filters.gte(
                    ((Filter.FieldFilterElement) element).field,
                    ((Filter.FieldFilterElement) element).values[0]
            );
            case LT -> Filters.lt(
                    ((Filter.FieldFilterElement) element).field,
                    ((Filter.FieldFilterElement) element).values[0]
            );
            case LTE -> Filters.lte(
                    ((Filter.FieldFilterElement) element).field,
                    ((Filter.FieldFilterElement) element).values[0]
            );
            case TEXT -> Filters.text(
                    ((Filter.FieldFilterElement) element).field,
                    (String) ((Filter.FieldFilterElement) element).values[0]
            );
            case REGEX -> Filters.regex(
                    ((Filter.FieldFilterElement) element).field,
                    (String) ((Filter.FieldFilterElement) element).values[0]
            );
            case IN -> Filters.in(
                    ((Filter.FieldFilterElement) element).field,
                    ((Filter.FieldFilterElement) element).values
            );
            case NOT_IN -> Filters.notIn(
                    ((Filter.FieldFilterElement) element).field,
                    ((Filter.FieldFilterElement) element).values
            );
            case ELEM_MATCH -> Filters.elemMatch(
                    ((Filter.FieldFilterElement) element).field,
                    getFilter(filterChain)
            );
            default -> Filters.ALL;
        };
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

    public static Optional<Object> getPrimaryKeyValue(Object object, Field field) throws IOException {
        field.setAccessible(true);
        try {
            Object value = field.get(object);
            if (value == null) {
                return Optional.empty();
            } else {
                // numeric primary keys need to be > 0!
                if ((field.getType().equals(byte.class) || field.getType().equals(Byte.class)) && (byte) value <= 0)
                    return Optional.empty();
                if ((field.getType().equals(short.class) || field.getType().equals(Short.class)) && (short) value <= 0)
                    return Optional.empty();
                if ((field.getType().equals(int.class) || field.getType().equals(Integer.class)) && (int) value <= 0)
                    return Optional.empty();
                if ((field.getType().equals(long.class) || field.getType().equals(Long.class)) && (long) value <= 0)
                    return Optional.empty();
                if ((field.getType().equals(float.class) || field.getType().equals(Float.class)) && (float) value <= 0)
                    return Optional.empty();
                if ((field.getType().equals(double.class) || field.getType().equals(Double.class)) && (double) value <= 0)
                    return Optional.empty();
            }
            return Optional.of(value);
        } catch (IllegalAccessException iae) {
            throw new IOException(ID_FIELD_IS_NOT_ACCESSIBLE.getMessage());
        }
    }

    private static Pair<Object, ObjectFilter> createUniqueFilter(Object object, Field field) throws IOException {
        Object value = getPrimaryKeyValue(object, field).orElseThrow(() -> new IOException(ID_FILTER_VALUE_CAN_NOT_BE_NULL.getMessage()));
        return Pair.of(value, eq(field.getName(), value));
    }

}
