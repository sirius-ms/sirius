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

package de.unijena.bioinf.storage.db.nitrite;

import de.unijena.bioinf.storage.db.NoSQLDatabase;
import de.unijena.bioinf.storage.db.NoSQLFilter;
import de.unijena.bioinf.storage.db.NoSQLPOJO;
import org.apache.commons.lang3.tuple.Pair;
import org.dizitart.no2.*;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import javax.validation.constraints.NotNull;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NitriteDatabase implements NoSQLDatabase<ObjectFilter>, Closeable, AutoCloseable {

    // Prevent illegal reflective access warnings
    static {
        if (!NitriteDatabase.class.getModule().isNamed()) {
            ClassLoader.class.getModule().addOpens(ClassLoader.class.getPackageName(), NitriteDatabase.class.getModule());
        }
    }

    protected Path file;

    // NITRITE
    private final Nitrite db;
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

    public NitriteDatabase(@NotNull Path file, Class<?>... classes) {
        this.file = file;
        this.db = Nitrite.builder().filePath(file.toFile()).compressed().openOrCreate();

        for (Class<?> clazz : classes) {
            ObjectRepository<?> repository = this.db.getRepository(clazz);
            this.repositories.put(clazz, repository);
            Collection<Index> repoIndices = repository.listIndices();
            try {
                Field indexField = clazz.getField("index");
                List<Index> toDrop = new ArrayList<>();
                List<NoSQLPOJO.Index> toBuild = new ArrayList<>();
                for (NoSQLPOJO.Index index : (NoSQLPOJO.Index[]) indexField.get(null)) {
                    found:
                    {
                        for (Index repoIndex : repoIndices) {
                            if (Objects.equals(repoIndex.getField(), index.getField())) {
                                IndexType repoType = repoIndex.getIndexType();
                                NoSQLPOJO.IndexType iType = index.getType();
                                if ((repoType == IndexType.Unique && iType != NoSQLPOJO.IndexType.UNIQUE) ||
                                        (repoType == IndexType.NonUnique && iType != NoSQLPOJO.IndexType.NON_UNIQUE) ||
                                        (repoType == IndexType.Fulltext && iType != NoSQLPOJO.IndexType.FULL_TEXT)) {
                                    toDrop.add(repoIndex);
                                    toBuild.add(index);
                                }
                                break found;
                            }
                        }
                        toBuild.add(index);
                    }
                }

                for (Index index : toDrop) {
                    repository.dropIndex(index.getField());
                }

                for (NoSQLPOJO.Index index : toBuild) {
                    switch (index.getType()) {
                        case UNIQUE:
                            repository.createIndex(index.getField(), IndexOptions.indexOptions(IndexType.Unique));
                            break;
                        case NON_UNIQUE:
                            repository.createIndex(index.getField(), IndexOptions.indexOptions(IndexType.NonUnique));
                            break;
                        case FULL_TEXT:
                            repository.createIndex(index.getField(), IndexOptions.indexOptions(IndexType.Fulltext));
                            break;
                    }
                }
            } catch (NoSuchFieldException e) {
                // that's okay, no index in this case
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
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
    private <T> Pair<T[], ObjectRepository<T>> getRepository(Collection<T> objects) throws IOException {
        if (objects.isEmpty()) {
            return null;
        }
        T[] arr = (T[]) objects.toArray();
        Class<T> clazz = (Class<T>) arr[0].getClass();
        if (!this.repositories.containsKey(clazz)) {
            throw new IOException(clazz + " is not registered.");
        }
        return Pair.of(arr, (ObjectRepository<T>) this.repositories.get(clazz));
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
    public <T> int insertAll(Collection<T> objects) throws IOException {
        return this.write(() -> {
            Pair<T[], ObjectRepository<T>> pair = this.getRepository(objects);
            if (pair == null) {
                return 0;
            }
            return pair.getRight().insert(pair.getLeft()).getAffectedCount();
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
    public <T> int upsertAll(Collection<T> objects) throws IOException {
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
    public <T> Iterable<T> find(NoSQLFilter filter, Class<T> clazz) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            ObjectFilter of = getFilter(filter);
            return repo.find(of);
        });
    }

    @Override
    public <T> Iterable<T> find(NoSQLFilter filter, Class<T> clazz, int offset, int pageSize) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            ObjectFilter of = getFilter(filter);
            return repo.find(of, FindOptions.limit(offset, pageSize));
        });
    }

    @Override
    public <T> Iterable<T> find(NoSQLFilter filter, Class<T> clazz, String sortField, SortOrder sortOrder) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            ObjectFilter of = getFilter(filter);
            return repo.find(of, FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending));
        });
    }

    @Override
    public <T> Iterable<T> find(NoSQLFilter filter, Class<T> clazz, int offset, int pageSize, String sortField, SortOrder sortOrder) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            ObjectFilter of = getFilter(filter);
            return repo.find(of, FindOptions.sort(sortField, (sortOrder == SortOrder.ASCENDING) ? org.dizitart.no2.SortOrder.Ascending : org.dizitart.no2.SortOrder.Descending).thenLimit(offset, pageSize));
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

    public <T, P, C> Iterable<T> joinAllChildren(Class<T> clazz, Class<P> parentClass, Class<C> childClass, Iterable<P> parents, String foreignField, String targetField) {
        org.dizitart.no2.objects.Cursor<P> parentCursor = (org.dizitart.no2.objects.Cursor<P>) parents;
        return new NitriteJoinedIterable<>(clazz, parentClass, childClass, parentCursor, foreignField, targetField, this);
    }

    public <T, P, C> Iterable<T> joinChildren(Class<T> clazz, Class<P> parentClass, Class<C> childClass, NoSQLFilter childFilter, Iterable<P> parents, String foreignField, String targetField) {
        org.dizitart.no2.objects.Cursor<P> parentCursor = (org.dizitart.no2.objects.Cursor<P>) parents;
        return new NitriteFilteredJoinedIterable<>(clazz, parentClass, childClass, childFilter, parentCursor, foreignField, targetField, this);
    }

    @Override
    public <T> int count(NoSQLFilter filter, Class<T> clazz) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            ObjectFilter of = getFilter(filter);
            return repo.find(of).totalCount();
        });
    }

    @Override
    public <T> int count(NoSQLFilter filter, Class<T> clazz, int offset, int pageSize) throws IOException {
        return this.read(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            ObjectFilter of = getFilter(filter);
            return repo.find(of, FindOptions.limit(offset, pageSize)).totalCount();
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
    public <T> int remove(T object) throws IOException {
        return this.write(() -> {
            ObjectRepository<T> repo = this.getRepository(object);
            return repo.remove(object).getAffectedCount();
        });
    }

    @Override
    public <T> int removeAll(Collection<T> objects) throws IOException {
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
    public <T> int removeAll(NoSQLFilter filter, Class<T> clazz) throws IOException {
        return this.write(() -> {
            ObjectRepository<T> repo = this.getRepository(clazz);
            ObjectFilter of = getFilter(filter);
            return repo.remove(of).getAffectedCount();
        });
    }

    private ObjectFilter[] getAllFilterChildren(Deque<NoSQLFilter.FilterElement> filterChain) {
        List<ObjectFilter> children = new ArrayList<>();
        while (!filterChain.isEmpty()) {
            ObjectFilter next = getFilter(filterChain);
            children.add(next);
        }
        ObjectFilter[] arr = new ObjectFilter[children.size()];
        return children.toArray(arr);
    }

    private ObjectFilter getFilter(Deque<NoSQLFilter.FilterElement> filterChain) {
        if (filterChain.isEmpty()) {
            return ObjectFilters.ALL;
        }
        NoSQLFilter.FilterElement element = filterChain.pop();
        switch (element.filterType) {
            case AND:
                return ObjectFilters.and(getAllFilterChildren(filterChain));
            case OR:
                return ObjectFilters.or(getAllFilterChildren(filterChain));
            case NOT:
                return ObjectFilters.not(getFilter(filterChain));
            case EQ:
                return ObjectFilters.eq(
                        ((NoSQLFilter.FieldFilterElement) element).field,
                        ((NoSQLFilter.FieldFilterElement) element).values[0]
                );
            case GT:
                return ObjectFilters.gt(
                        ((NoSQLFilter.FieldFilterElement) element).field,
                        ((NoSQLFilter.FieldFilterElement) element).values[0]
                );
            case GTE:
                return ObjectFilters.gte(
                        ((NoSQLFilter.FieldFilterElement) element).field,
                        ((NoSQLFilter.FieldFilterElement) element).values[0]
                );
            case LT:
                return ObjectFilters.lt(
                        ((NoSQLFilter.FieldFilterElement) element).field,
                        ((NoSQLFilter.FieldFilterElement) element).values[0]
                );
            case LTE:
                return ObjectFilters.lte(
                        ((NoSQLFilter.FieldFilterElement) element).field,
                        ((NoSQLFilter.FieldFilterElement) element).values[0]
                );
            case TEXT:
                return ObjectFilters.text(
                        ((NoSQLFilter.FieldFilterElement) element).field,
                        (String) ((NoSQLFilter.FieldFilterElement) element).values[0]
                );
            case REGEX:
                return ObjectFilters.regex(
                        ((NoSQLFilter.FieldFilterElement) element).field,
                        (String) ((NoSQLFilter.FieldFilterElement) element).values[0]
                );
            case IN:
                return ObjectFilters.in(
                        ((NoSQLFilter.FieldFilterElement) element).field,
                        ((NoSQLFilter.FieldFilterElement) element).values
                );
            case NOT_IN:
                return ObjectFilters.notIn(
                        ((NoSQLFilter.FieldFilterElement) element).field,
                        ((NoSQLFilter.FieldFilterElement) element).values
                );
            case ELEM_MATCH:
                return ObjectFilters.elemMatch(
                        ((NoSQLFilter.FieldFilterElement) element).field,
                        getFilter(filterChain)
                );
        }
        return ObjectFilters.ALL;
    }

    @Override
    public ObjectFilter getFilter(NoSQLFilter filter) {
        return getFilter(new ArrayDeque<>(filter.filterChain));
    }

}
