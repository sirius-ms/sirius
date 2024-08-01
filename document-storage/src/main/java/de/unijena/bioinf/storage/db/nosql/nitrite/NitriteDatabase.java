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
import de.unijena.bioinf.storage.db.nosql.*;
import de.unijena.bioinf.storage.db.nosql.nitrite.joining.JoinedReflectionIterable;
import de.unijena.bioinf.storage.db.nosql.nitrite.projection.InjectedDocumentStream;
import de.unijena.bioinf.storage.db.nosql.nitrite.projection.InjectedObjectStream;
import de.unijena.bioinf.storage.db.nosql.nitrite.projection.OptFieldDocumentStream;
import de.unijena.bioinf.storage.db.nosql.utils.PKSuppliers;
import io.hypersistence.tsid.TSID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.collection.*;
import org.dizitart.no2.collection.events.CollectionEventListener;
import org.dizitart.no2.collection.events.EventType;
import org.dizitart.no2.common.PersistentCollection;
import org.dizitart.no2.common.RecordStream;
import org.dizitart.no2.common.WriteResult;
import org.dizitart.no2.common.mapper.JacksonMapperModule;
import org.dizitart.no2.common.mapper.NitriteMapper;
import org.dizitart.no2.common.module.NitriteModule;
import org.dizitart.no2.common.processors.ProcessorChain;
import org.dizitart.no2.filters.FluentFilter;
import org.dizitart.no2.filters.NitriteFilter;
import org.dizitart.no2.index.IndexDescriptor;
import org.dizitart.no2.index.IndexOptions;
import org.dizitart.no2.mvstore.MVStoreModule;
import org.dizitart.no2.repository.ObjectRepository;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

@Slf4j
public class NitriteDatabase implements Database<Document> {
    private final Metadata meta;

    public enum MVStoreCompression{NONE, LZF, DEFLATE}

    protected Path file;

    // NITRITE
    private final Nitrite db;

    @Getter
    private final NitriteMapper nitriteMapper;

    private final Map<Class<?>, ObjectRepository<?>> repositories = Collections.synchronizedMap(new HashMap<>());

    private final Map<Class<?>, Set<String>> optionalRepoFields = Collections.synchronizedMap(new HashMap<>());

    private final Map<Class<?>, Field> primaryKeyFields = Collections.synchronizedMap(new HashMap<>());

    private final Map<Class<?>, Supplier<?>> primaryKeySuppliers = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, NitriteCollection> collections = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, Set<String>> optionalCollectionFields = Collections.synchronizedMap(new HashMap<>());

    private final Map<Long, CollectionEventListener> listeners = Collections.synchronizedMap(new HashMap<>());

    // LOCKS
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();
    private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
    private final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock stateWriteLock = stateLock.writeLock();
    private final ReentrantReadWriteLock.ReadLock stateReadLock = stateLock.readLock();

    // STATE
    public NitriteDatabase(Path file, Metadata meta) throws IOException {
        this(file, meta, MVStoreCompression.LZF, 64, 8192);
    }

    public NitriteDatabase(Path file, Metadata meta, MVStoreCompression compression, int cacheSizeMiB, int commitBufferByte) throws IOException {
        this.file = file;
        this.meta = meta;
        this.db = initDB(file, meta, compression, cacheSizeMiB,commitBufferByte);
        this.initCollections(meta);
        this.initRepositories(meta);
        this.initOptionalFields(meta);
        this.nitriteMapper = this.db.getConfig().nitriteMapper();
    }

    private Nitrite initDB(Path file, Metadata meta, MVStoreCompression compress, int cacheSizeMiB, int commitBufferByte) {
        SimpleModule module = new SimpleModule("sirius-nitrite", Version.unknownVersion());
        for (Map.Entry<Class<?>, JsonSerializer<?>> entry : meta.serializers.entrySet()) {
            addSerializer(module, entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Class<?>, JsonDeserializer<?>> entry : meta.deserializers.entrySet()) {
            addDeserializer(module, entry.getKey(), entry.getValue());
        }
        NitriteModule storeModule = MVStoreModule.withConfig().filePath(file.toFile())
                .compress(compress == MVStoreCompression.LZF)
                .compressHigh(compress == MVStoreCompression.DEFLATE)
                .autoCommitBufferSize(commitBufferByte) //8kib for 2048 and lower there is a weired bug in Nitrite + MvStore where the db crashed during close operation in 2% of the cases.
                .cacheSize(cacheSizeMiB)
                .build();
//        NitriteModule storeModule = RocksDBModule.withConfig().filePath(file.toFile()).build();

        return Nitrite.builder().loadModule(storeModule)
                .loadModule(new JacksonMapperModule(module))
                .openOrCreate();
    }

    private void initCollections(Metadata meta) {
        for (String name : meta.collectionIndices.keySet()) {
            NitriteCollection collection = this.db.getCollection(name);
            this.collections.put(name, collection);
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

    /**
     * Allows to clear a repository by dropping and recreating it
     */
    public void clearRepository(Class<?> clazz, boolean rebuildIndex) throws IOException {
        stateWriteLock.lock();
        try {
            this.db.getRepository(clazz).drop();
            ObjectRepository<?> repository = this.db.getRepository(clazz);
            this.repositories.put(clazz, repository);
            initPrimaryKey(clazz, meta.pkFields.get(clazz), meta.pkSuppliers.get(clazz), repository);
            if (rebuildIndex) {
                initIndex(meta.repoIndices.get(clazz), repository);
            }
        } finally {
            stateWriteLock.unlock();
        }
    }

    @Override
    public void disableIndices(Class<?> clazz, Index... keep) {
        HashSet<Index> kp = new HashSet<>(Arrays.asList(keep));
        stateWriteLock.lock();
        try {
            String pkField = meta.pkFields.get(clazz).getName();
            Index[] pkIndexToKeep = Arrays.stream(meta.repoIndices.get(clazz))
                    .filter(i -> kp.contains(i) || i.getFields()[0].equals(pkField)).toArray(Index[]::new);
            initIndex(pkIndexToKeep, this.db.getRepository(clazz));
        } finally {
            stateWriteLock.unlock();
        }
    }

    @Override
    public void enableIndices(Class<?> clazz) {
        stateWriteLock.lock();
        try {
            initIndex(meta.repoIndices.get(clazz), this.db.getRepository(clazz));
        } finally {
            stateWriteLock.unlock();
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
            repository.createIndex(IndexOptions.indexOptions(org.dizitart.no2.index.IndexType.UNIQUE), pkName);
        }
    }

    private <Repo extends PersistentCollection<?>> void initIndex(Index[] indices, Repo repository) {
        List<IndexDescriptor> toDrop = new ArrayList<>();
        List<Index> toBuild = new ArrayList<>();

        Map<Set<String>, IndexDescriptor> rI = new HashMap<>();
        Map<Set<String>, Index> nI = new HashMap<>();

        for (IndexDescriptor descriptor : repository.listIndices()) {
            rI.put(new HashSet<>(descriptor.getFields().getFieldNames()), descriptor);
        }
        for (Index index : indices) {
            Set<String> key = new HashSet<>(Arrays.asList(index.getFields()));
            if (nI.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate index: " + String.join(",", key));
            }
            nI.put(key, index);
        }

        Set<Set<String>> intersection = new HashSet<>(rI.keySet());
        intersection.retainAll(nI.keySet());

        Set<Set<String>> dropSet = new HashSet<>(rI.keySet());
        dropSet.removeAll(nI.keySet());

        Set<Set<String>> addSet = new HashSet<>(nI.keySet());
        addSet.removeAll(rI.keySet());

        dropSet.stream().map(rI::get).forEach(toDrop::add);
        addSet.stream().map(nI::get).forEach(toBuild::add);

        // compare index types
        for (Set<String> key : intersection) {
            IndexDescriptor repoIndex = rI.get(key);
            Index index = nI.get(key);

            String repoType = repoIndex.getIndexType();
            IndexType iType = index.getType();
            if ((Objects.equals(repoType, org.dizitart.no2.index.IndexType.UNIQUE) && iType != IndexType.UNIQUE) ||
                    (Objects.equals(repoType, org.dizitart.no2.index.IndexType.NON_UNIQUE) && iType != IndexType.NON_UNIQUE) ||
                    (Objects.equals(repoType, org.dizitart.no2.index.IndexType.FULL_TEXT) && iType != IndexType.FULL_TEXT)) {
                toDrop.add(repoIndex);
                toBuild.add(index);
            }
        }

        for (IndexDescriptor index : toDrop) {
            log.info("Dropping index: {}", Arrays.toString(index.getFields().getFieldNames().toArray(new String[0])));
            repository.dropIndex(index.getFields().getFieldNames().toArray(String[]::new));
        }

        for (Index index : toBuild) {
            log.info("(Re)building index: {}", Arrays.toString(index.getFields()));
            switch (index.getType()) {
                case UNIQUE ->
                        repository.createIndex(IndexOptions.indexOptions(org.dizitart.no2.index.IndexType.UNIQUE), index.getFields());
                case NON_UNIQUE ->
                        repository.createIndex(IndexOptions.indexOptions(org.dizitart.no2.index.IndexType.NON_UNIQUE), index.getFields());
                case FULL_TEXT ->
                        repository.createIndex(IndexOptions.indexOptions(org.dizitart.no2.index.IndexType.FULL_TEXT), index.getFields());
            }
        }
    }

    @Override
    public void flush() {
        stateReadLock.lock();
        try { //flush should work like close an not throw an exception if the database is closed?
            if (!this.db.isClosed())
                this.db.commit();
            else
                LoggerFactory.getLogger(getClass()).warn("Nitrite database is closed! Cannot commit any changes!");
        } finally {
            stateReadLock.unlock();
        }
    }

    @Override
    public void close() {
        stateWriteLock.lock();
        try {
            if (!this.db.isClosed())
                this.db.close();
        } finally {
            stateWriteLock.unlock();
        }
    }

    private <T> T callIfOpen(Callable<T> callable) throws IOException {
        stateReadLock.lock();
        if (this.db.isClosed()) {
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

    public  <T> T read(Callable<T> callable) throws IOException {
        return this.callIfOpen(() -> {
            readLock.lock();
            try {
                return callable.call();
            } finally {
                readLock.unlock();
            }
        });
    }

    public <T> T write(Callable<T> callable) throws IOException {
        return this.callIfOpen(() -> {
            writeLock.lock();
            try {
                return callable.call();
            } finally {
                writeLock.unlock();
            }
        });
    }

    @Override
    public Set<Class<?>> getAllRegisteredClasses() {
        return this.repositories.keySet();
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
        List<T> collection = new ArrayList<>();
        objects.forEach(collection::add);

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
        if (!collections.containsKey(name)) {
            if (!db.hasCollection(name) && !db.listRepositories().contains(name)) {
                throw new IOException(name + " is not registered.");
            }

            return db.getCollection(name);
        }

        return collections.get(name);
    }

    @SuppressWarnings("unchecked")
    private <T> T maybeProject(Class<T> clazz, Document document, String[] withOptionalFields) {
        Set<String> omittedFields = new HashSet<>(optionalRepoFields.containsKey(clazz) ? optionalRepoFields.get(clazz) : Set.of());
        omittedFields.removeAll(new HashSet<>(Arrays.asList(withOptionalFields)));
        Document projected = OptFieldDocumentStream.project(document, omittedFields, new ProcessorChain());
        return (T) nitriteMapper.tryConvert(projected, clazz);
    }

    private Document maybeProjectDocument(String collectionName, Document document, String[] withOptionalFields) {
        Set<String> omittedFields = new HashSet<>(optionalCollectionFields.containsKey(collectionName) ? optionalCollectionFields.get(collectionName) : Set.of());
        omittedFields.removeAll(new HashSet<>(Arrays.asList(withOptionalFields)));
        return OptFieldDocumentStream.project(document, omittedFields, new ProcessorChain());
    }

    private <T> Iterable<T> maybeProject(Class<T> clazz, @Nullable Filter filter, @Nullable FindOptions findOptions, String[] withOptionalFields) throws IOException {
        Set<String> omittedFields = new HashSet<>(optionalRepoFields.containsKey(clazz) ? optionalRepoFields.get(clazz) : Set.of());
        omittedFields.removeAll(new HashSet<>(Arrays.asList(withOptionalFields)));

        DocumentCursor cursor = doFindDocument(getRepository(clazz).getDocumentCollection(), filter, findOptions);
        RecordStream<Document> recordStream = CustomDocumentStream.of(cursor).project(omittedFields);
        return new CustomObjectCursor<>(nitriteMapper, recordStream, cursor.getFindPlan(), clazz);
    }

    private <T> Iterable<Document> maybeProjectWithoutConvert(Class<T> clazz, @Nullable Filter filter, @Nullable FindOptions findOptions, String[] withOptionalFields) throws IOException {
        Set<String> omittedFields = new HashSet<>(optionalRepoFields.containsKey(clazz) ? optionalRepoFields.get(clazz) : Set.of());
        omittedFields.removeAll(new HashSet<>(Arrays.asList(withOptionalFields)));

        DocumentCursor cursor = doFindDocument(getRepository(clazz).getDocumentCollection(), filter, findOptions);
        return CustomDocumentStream.of(cursor).project(omittedFields);
    }

    private RecordStream<Document> maybeProjectDocuments(String collectionName, @Nullable Filter filter, @Nullable FindOptions findOptions, String[] withOptionalFields) throws IOException {
        Set<String> omittedFields = new HashSet<>(optionalCollectionFields.containsKey(collectionName) ? optionalCollectionFields.get(collectionName) : Set.of());
        omittedFields.removeAll(new HashSet<>(Arrays.asList(withOptionalFields)));

        DocumentCursor cursor = doFindDocument(getCollection(collectionName), filter, findOptions);
        return CustomDocumentStream.of(cursor).project(omittedFields);
    }

    private FindOptions translateSort(String[] sortFields, SortOrder[] sortOrder) {
        if (sortFields.length == sortOrder.length && sortFields.length > 0) {
            FindOptions options = FindOptions.orderBy(sortFields[0], (sortOrder[0] == SortOrder.ASCENDING) ? org.dizitart.no2.common.SortOrder.Ascending : org.dizitart.no2.common.SortOrder.Descending);
            for (int i = 1; i < sortFields.length; i++) {
                options.thenOrderBy(sortFields[i], (sortOrder[i] == SortOrder.ASCENDING) ? org.dizitart.no2.common.SortOrder.Ascending : org.dizitart.no2.common.SortOrder.Descending);
            }
            return options;
        } else {
            return new FindOptions();
        }
    }

    private <T> Iterable<T> doFind(Class<T> clazz, @Nullable Filter filter, @Nullable FindOptions findOptions) throws IOException {
        if (filter != null && findOptions != null) {
            return getRepository(clazz).find(getFilter(filter), findOptions);
        } else if (filter != null) {
            return getRepository(clazz).find(getFilter(filter));
        } else if (findOptions != null) {
            return getRepository(clazz).find(findOptions);
        } else {
            return getRepository(clazz).find();
        }
    }

    private DocumentCursor doFindDocument(NitriteCollection collection, @Nullable Filter filter, @Nullable FindOptions findOptions) throws IOException {
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
                    throw new IOException("id can not be null");
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
                        throw new IOException("id can not be null");
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
            WriteResult result = collection.insert(document);
            if (result.iterator().hasNext()) {
                document.put("_id", result.iterator().next().getIdValue());
            }
            return result.getAffectedCount();
        });
    }

    @Override
    public int insertAll(String collectionName, Iterable<Document> documents) throws IOException {
        return this.write(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            Document[] docs = StreamSupport.stream(documents.spliterator(),false).toArray(Document[]::new);
            WriteResult result = collection.insert(docs);
            List<String> ids = StreamSupport.stream(result.spliterator(), false).map(NitriteId::getIdValue).toList();
            if (ids.size() == docs.length) {
                for (int i = 0; i < docs.length; i++) {
                    docs[i].put("_id", ids.get(i));
                }
            }
            return result.getAffectedCount();
        });
    }

    @Override
    public <T> int upsert(T object) throws IOException {
        return this.write(() -> {
            ObjectRepository<T> repo = this.getRepository(object);
            Pair<Object, NitriteFilter> filter = createUniqueFilter(object, primaryKeyFields.get(object.getClass()));
            if (filter.getLeft() != null) {
                return repo.update(filter.getRight(), object, UpdateOptions.updateOptions(true)).getAffectedCount();
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
                Pair<Object, NitriteFilter> filter = createUniqueFilter(o, primaryKeyFields.get(o.getClass()));
                if (filter.getLeft() != null) {
                    count += triple.getMiddle().update(filter.getRight(), o, UpdateOptions.updateOptions(true)).getAffectedCount();
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
            List<T> results = Lists.newArrayList(maybeProject(clazz, Filter.where(this.primaryKeyFields.get(clazz).getName()).eq(primaryKey), null, withOptionalFields).iterator());
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
            return Optional.of(maybeProjectDocument(collectionName, document, withOptionalFields));
        });
    }

    @Override
    public <T> Iterable<T> find(Filter filter, Class<T> clazz, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, filter, null, withOptionalFields));
    }

    @Override
    public <T> Iterable<T> find(Filter filter, Class<T> clazz, long offset, int pageSize, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, filter, FindOptions.skipBy(offset).limit(pageSize), withOptionalFields));
    }

    @Override
    public <T> Iterable<T> find(Filter filter, Class<T> clazz, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, filter, FindOptions.orderBy(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.common.SortOrder.Ascending : org.dizitart.no2.common.SortOrder.Descending), withOptionalFields));
    }

    @Override
    public <T> Iterable<T> find(Filter filter, Class<T> clazz, String[] sortFields, SortOrder[] sortOrders, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, filter, translateSort(sortFields, sortOrders), withOptionalFields));
    }

    @Override
    public <T> Iterable<T> find(Filter filter, Class<T> clazz, long offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, filter, FindOptions.orderBy(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.common.SortOrder.Ascending : org.dizitart.no2.common.SortOrder.Descending).skip(offset).limit(pageSize), withOptionalFields));
    }

    @Override
    public <T> Iterable<T> find(Filter filter, Class<T> clazz, long offset, int pageSize, String[] sortFields, SortOrder[] sortOrders, String... withOptionalFields) throws IOException {
        FindOptions options = translateSort(sortFields, sortOrders);
        return this.read(() -> maybeProject(clazz, filter, options.skip(offset).limit(pageSize), withOptionalFields));
    }

    @Override
    public Iterable<Document> find(String collectionName, Filter filter, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, filter, null, withOptionalFields));
    }

    @Override
    public Iterable<Document> find(String collectionName, Filter filter, long offset, int pageSize, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, filter, FindOptions.skipBy(offset).limit(pageSize), withOptionalFields));
    }

    @Override
    public Iterable<Document> find(String collectionName, Filter filter, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, filter, FindOptions.orderBy(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.common.SortOrder.Ascending : org.dizitart.no2.common.SortOrder.Descending), withOptionalFields));
    }

    @Override
    public Iterable<Document> find(String collectionName, Filter filter, long offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, filter, FindOptions.orderBy(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.common.SortOrder.Ascending : org.dizitart.no2.common.SortOrder.Descending).skip(offset).limit(pageSize), withOptionalFields));
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> clazz, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, null, null, withOptionalFields));
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> clazz, long offset, int pageSize, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, null, FindOptions.skipBy(offset).limit(pageSize), withOptionalFields));
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> clazz, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, null, FindOptions.orderBy(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.common.SortOrder.Ascending : org.dizitart.no2.common.SortOrder.Descending), withOptionalFields));
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> clazz, String[] sortFields, SortOrder[] sortOrders, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, null, translateSort(sortFields, sortOrders), withOptionalFields));
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> clazz, long offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProject(clazz, null, FindOptions.orderBy(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.common.SortOrder.Ascending : org.dizitart.no2.common.SortOrder.Descending).skip(offset).limit(pageSize), withOptionalFields));
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> clazz, long offset, int pageSize, String[] sortFields, SortOrder[] sortOrders, String... withOptionalFields) throws IOException {
        FindOptions options = translateSort(sortFields, sortOrders);
        return this.read(() -> maybeProject(clazz, null, options.skip(offset).limit(pageSize), withOptionalFields));
    }

    @Override
    public Iterable<Document> findAll(String collectionName, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, null, null, withOptionalFields));
    }

    @Override
    public Iterable<Document> findAll(String collectionName, long offset, int pageSize, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, null, FindOptions.skipBy(offset).limit(pageSize), withOptionalFields));
    }

    @Override
    public Iterable<Document> findAll(String collectionName, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, null, FindOptions.orderBy(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.common.SortOrder.Ascending : org.dizitart.no2.common.SortOrder.Descending), withOptionalFields));
    }

    @Override
    public Iterable<Document> findAll(String collectionName, long offset, int pageSize, String sortField, SortOrder sortOrder, String... withOptionalFields) throws IOException {
        return this.read(() -> maybeProjectDocuments(collectionName, null, FindOptions.orderBy(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.common.SortOrder.Ascending : org.dizitart.no2.common.SortOrder.Descending).skip(offset).limit(pageSize), withOptionalFields));
    }

    @Override
    public <T> T injectOptionalFields(T object, String... optionalFields) throws IOException {
        return InjectedObjectStream.inject(object, new HashSet<>(Arrays.asList(optionalFields)), getRepository(object.getClass()).getDocumentCollection(), primaryKeyFields.get(object.getClass()), nitriteMapper);
    }

    @Override
    public Document injectOptionalFields(String collectionName, Document document, String... optionalFields) throws IOException {
        return InjectedDocumentStream.inject(document, new HashSet<>(Arrays.asList(optionalFields)), getCollection(collectionName), new ProcessorChain());
    }

    @Override
    public <T> Iterable<T> injectOptionalFields(Class<T> clazz, Iterable<T> objects, String... optionalFields) throws IOException {
        if (objects instanceof CustomObjectCursor<T> cursor) {
            RecordStream<Document> documents = cursor.getRecordStream();
            RecordStream<Document> injected = CustomDocumentStream.of(documents).inject(new HashSet<>(Arrays.asList(optionalFields)), getRepository(clazz).getDocumentCollection());
            return new CustomObjectCursor<>(nitriteMapper, injected, cursor.getFindPlan(), clazz);
        }
        return new InjectedObjectStream<>(objects, new HashSet<>(Arrays.asList(optionalFields)), getRepository(clazz).getDocumentCollection(), primaryKeyFields.get(clazz), nitriteMapper);
    }

    @Override
    public Iterable<Document> injectOptionalFields(String collectionName, Iterable<Document> documents, String... optionalFields) throws IOException {
        return CustomDocumentStream.of(documents).inject(new HashSet<>(Arrays.asList(optionalFields)), getCollection(collectionName));
    }

    private <P, C> Iterable<P> join(Class<C> childClass, Iterable<P> parents, Function<Object, Iterable<Document>> children, String localField, String targetField) throws IOException {
        if (parents instanceof CustomObjectCursor<P> cursor) {
            RecordStream<Document> parentDocuments = cursor.getRecordStream();
            return new CustomObjectCursor<>(nitriteMapper,
                    CustomDocumentStream.of(parentDocuments).join(
                            children,
                            localField,
                            targetField),
                    cursor.getFindPlan(),
                    cursor.getType());
        } else {
            return new JoinedReflectionIterable<>(childClass, parents, children, localField, targetField, nitriteMapper);
        }
    }

    @Override
    public <P, C> Iterable<P> joinAllChildren(Class<C> childClass, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return join(childClass, parents, (localObject) -> {
            try {
                return maybeProjectWithoutConvert(childClass, Filter.where(foreignField).eq(localObject), null, withOptionalChildFields);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, localField, targetField);
    }

    @Override
    public <P, C> Iterable<P> joinChildren(Class<C> childClass, Filter childFilter, Iterable<P> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return join(childClass, parents, (localObject) -> {
            try {
                Filter.FilterNode root = (childFilter instanceof Filter.FilterClause) ? (Filter.FilterNode) childFilter : childFilter.getParent();
                while (root.getParent() != null)
                    root = root.getParent();
                Iterable<Document> result = maybeProjectWithoutConvert(childClass, Filter.and(childFilter, Filter.where(foreignField).eq(localObject)), null, withOptionalChildFields);
                root.setParent(null);
                return result;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, localField, targetField);
    }

    @Override
    public Iterable<Document> joinAllChildren(String childCollectionName, Iterable<Document> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return CustomDocumentStream.of(parents).join(
                (localObject) -> {
                    try {
                        return find(childCollectionName, Filter.where(foreignField).eq(localObject), withOptionalChildFields);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                localField,
                targetField);
    }

    @Override
    public Iterable<Document> joinChildren(String childCollectionName, Filter childFilter, Iterable<Document> parents, String localField, String foreignField, String targetField, String... withOptionalChildFields) throws IOException {
        return CustomDocumentStream.of(parents).join(
                (localObject) -> {
                    try {
                        Filter.FilterNode root = (childFilter instanceof Filter.FilterClause) ? (Filter.FilterNode) childFilter : childFilter.getParent();
                        while (root.getParent() != null)
                            root = root.getParent();
                        Iterable<Document> c = find(childCollectionName, Filter.and(childFilter, Filter.where(foreignField).eq(localObject)), withOptionalChildFields);
                        root.setParent(null);
                        return c;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                localField,
                targetField);
    }


    @Override
    public <T> boolean containsPrimaryKey(Object primaryKey, Class<T> clazz) throws IOException {
        return count(Filter.where(this.primaryKeyFields.get(clazz).getName()).eq(primaryKey), clazz) > 0;
    }
    @Override
    public <T> long count(Filter filter, Class<T> clazz) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            NitriteFilter f = getFilter(filter);
            return repo.find(f).size();
        });
    }

    @Override
    public <T> long count(Filter filter, Class<T> clazz, long offset, int pageSize) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            NitriteFilter f = getFilter(filter);
            return repo.find(f, FindOptions.skipBy(offset).limit(pageSize)).size();
        });
    }

    @Override
    public <T> long countAll(Class<T> clazz) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            return repo.size();
        });
    }

    @Override
    public long count(String collectionName, Filter filter) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            NitriteFilter f = getFilter(filter);
            return collection.find(f).size();
        });
    }

    @Override
    public long count(String collectionName, Filter filter, long offset, int pageSize) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            NitriteFilter f = getFilter(filter);
            return collection.find(f, FindOptions.skipBy(offset).limit(pageSize)).size();
        });
    }

    @Override
    public long countAll(String collectionName) throws IOException {
        return this.read(() -> {
            NitriteCollection collection = this.getCollection(collectionName);
            return collection.size();
        });
    }

    @Override
    public <T> int remove(T object) throws IOException {
        return this.write(() -> {
            ObjectRepository<T> repo = this.getRepository(object);
            Pair<Object, NitriteFilter> pair = createUniqueFilter(object, primaryKeyFields.get(object.getClass()));
            return repo.remove(pair.getRight()).getAffectedCount();
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
                Pair<Object, NitriteFilter> pair = createUniqueFilter(o, primaryKeyFields.get(o.getClass()));
                count += triple.getMiddle().remove(pair.getRight()).getAffectedCount();
            }
            return count;
        });
    }

    @Override
    public <T> int removeAll(Filter filter, Class<T> clazz) throws IOException {
        return this.write(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            NitriteFilter f = getFilter(filter);
            return repo.remove(f).getAffectedCount();
        });
    }

    @Override
    public <T> boolean removeOne(Filter filter, Class<T> clazz) throws IOException {
        return this.write(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            NitriteFilter f = getFilter(filter);
            return repo.remove(f, true).getAffectedCount() > 0;
        });
    }

    @Override
    public <T> int removeByPrimaryKey(Object primaryKey, Class<T> clazz) throws IOException {
        return removeAll(Filter.where(this.primaryKeyFields.get(clazz).getName()).eq(primaryKey), clazz);
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
            NitriteFilter f = getFilter(filter);
            return collection.remove(f).getAffectedCount();
        });
    }

    @Override
    public <T> long onInsert(Class<T> clazz, Consumer<T> listener, String... withOptionalFields) throws IOException {
        return registerListener(EventType.Insert, clazz, listener, withOptionalFields);
    }

    @Override
    public long onInsert(String collectionName, Consumer<Document> listener, String... withOptionalFields) throws IOException {
        return registerListener(EventType.Insert, collectionName, listener, withOptionalFields);
    }

    @Override
    public <T> long onUpdate(Class<T> clazz, Consumer<T> listener, String... withOptionalFields) throws IOException {
        return registerListener(EventType.Update, clazz, listener, withOptionalFields);
    }

    @Override
    public long onUpdate(String collectionName, Consumer<Document> listener, String... withOptionalFields) throws IOException {
        return registerListener(EventType.Update, collectionName, listener, withOptionalFields);
    }

    @Override
    public <T> long onRemove(Class<T> clazz, Consumer<T> listener, String... withOptionalFields) throws IOException {
        return registerListener(EventType.Remove, clazz, listener, withOptionalFields);
    }

    @Override
    public long onRemove(String collectionName, Consumer<Document> listener, String... withOptionalFields) throws IOException {
        return registerListener(EventType.Remove, collectionName, listener, withOptionalFields);
    }

    private <T> long registerListener(EventType eventType, Class<T> clazz, Consumer<T> listener, String[] withOptionalFields) throws IOException {
        CollectionEventListener wrapper = eventInfo -> {
            if (eventInfo.getEventType().equals(eventType)) {
                Document doc = (Document) eventInfo.getItem();
                if (doc != null) {
                    listener.accept(maybeProject(clazz, doc, withOptionalFields));
                }
            }
        };
        final long tsid = TSID.fast().toLong();
        this.listeners.put(tsid, wrapper);
        getRepository(clazz).subscribe(wrapper);
        return tsid;
    }

    private long registerListener(EventType eventType, String collectionName, Consumer<Document> listener, String[] withOptionalFields) throws IOException {
        CollectionEventListener wrapper = eventInfo -> {
            if (eventInfo.getEventType().equals(eventType)) {
                Document doc = (Document) eventInfo.getItem();
                if (doc != null) {
                    listener.accept(maybeProjectDocument(collectionName, doc, withOptionalFields));
                }
            }
        };
        final long tsid = TSID.fast().toLong();
        this.listeners.put(tsid, wrapper);
        getCollection(collectionName).subscribe(wrapper);
        return tsid;
    }

    @Override
    public void unsubscribe(Class<?> clazz, long listenerId) throws IOException {
        getRepository(clazz).unsubscribe(this.listeners.get(listenerId));
        this.listeners.remove(listenerId);
    }

    @Override
    public void unsubscribe(String collectionName, long listenerId) throws IOException {
        getCollection(collectionName).unsubscribe(this.listeners.get(listenerId));
        this.listeners.remove(listenerId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public NitriteFilter getFilter(Filter filter) {
        Filter.FilterNode root = (filter instanceof Filter.FilterClause) ? (Filter.FilterNode) filter : filter.getParent();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        return filterRecursion(root);
    }

    private NitriteFilter addLiteralRecursive(Filter.FilterNode[] children, int index, NitriteFilter nf, Filter.FilterClause.Type type) {
        if (index < 0) {
            return nf;
        } else if (nf == null) {
            nf = filterRecursion(children[index]);
        } else if (type == Filter.FilterClause.Type.AND) {
            nf = (NitriteFilter) filterRecursion(children[index]).and(nf);
        } else if (type == Filter.FilterClause.Type.OR) {
            nf = (NitriteFilter) filterRecursion(children[index]).or(nf);
        } else {
            throw new IllegalArgumentException();
        }
        return addLiteralRecursive(children, index - 1, nf, type);
    }

    private NitriteFilter filterRecursion(Filter.FilterNode node) {
        if (node instanceof Filter.FilterLiteral literal) {
            return switch (literal.getType()) {
                case EQ -> FluentFilter.where(literal.getField()).eq(literal.getValues()[0]);
                case NOT_EQ -> FluentFilter.where(literal.getField()).notEq(literal.getValues()[0]);
                case GT -> FluentFilter.where(literal.getField()).gt((Comparable<?>) literal.getValues()[0]);
                case GTE -> FluentFilter.where(literal.getField()).gte((Comparable<?>) literal.getValues()[0]);
                case LT -> FluentFilter.where(literal.getField()).lt((Comparable<?>) literal.getValues()[0]);
                case LTE -> FluentFilter.where(literal.getField()).lte((Comparable<?>) literal.getValues()[0]);
                case BETWEEN ->
                        FluentFilter.where(literal.getField()).between((Comparable<?>) literal.getValues()[0], (Comparable<?>) literal.getValues()[1], (boolean) literal.getValues()[2], (boolean) literal.getValues()[3]);
                case TEXT -> FluentFilter.where(literal.getField()).text((String) literal.getValues()[0]);
                case REGEX -> FluentFilter.where(literal.getField()).regex((String) literal.getValues()[0]);
                case IN ->
                        FluentFilter.where(literal.getField()).in(Arrays.stream(literal.getValues()).map(o -> (Comparable<?>) o).toArray(Comparable[]::new));
                case NOT_IN ->
                        FluentFilter.where(literal.getField()).notIn(Arrays.stream(literal.getValues()).map(o -> (Comparable<?>) o).toArray(Comparable[]::new));
                case ELEM_MATCH ->
                        FluentFilter.where(literal.getField()).elemMatch(filterRecursion((Filter.FilterNode) literal.getValues()[0]));
            };
        } else if (node instanceof Filter.FilterClause clause) {
            Filter.FilterNode[] children = clause.getChildren();
            return addLiteralRecursive(children, children.length - 1, null, clause.getType());
        } else {
            throw new IllegalArgumentException(node.getClass().getName());
        }
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
            throw new IOException("id field is not accessible");
        }
    }

    private static Pair<Object, NitriteFilter> createUniqueFilter(Object object, Field field) throws IOException {
        Object value = getPrimaryKeyValue(object, field).orElseThrow(() -> new IOException("id can not be null"));
        return Pair.of(value, FluentFilter.where(field.getName()).eq(value));
    }

}
