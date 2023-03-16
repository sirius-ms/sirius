package de.unijena.bioinf.storage.db;

import java.io.IOException;
import java.util.Collection;

public interface NoSQLDatabase<F> {

    enum SortOrder {
        ASCENDING, DESCENDING
    }

    // CRUD operations

    <T> int insert(T object) throws IOException;

    <T> int insertAll(Collection<T> objects) throws IOException;

    <T> int upsert(T object) throws IOException;

    <T> int upsertAll(Collection<T> objects) throws IOException;

    <T> Iterable<T> find(NoSQLFilter filter, Class<T> clazz) throws IOException;

    <T> Iterable<T> find(NoSQLFilter filter, Class<T> clazz, int offset, int pageSize) throws IOException;

    <T> Iterable<T> find(NoSQLFilter filter, Class<T> clazz, String sortField, SortOrder sortOrder) throws IOException;

    <T> Iterable<T> find(NoSQLFilter filter, Class<T> clazz, int offset, int pageSize, String sortField, SortOrder sortOrder) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, int offset, int pageSize) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, String sortField, SortOrder sortOrder) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, int offset, int pageSize, String sortField, SortOrder sortOrder) throws IOException;

    <T, L, R> Iterable<T> join(Class<T> clazz, Iterable<L> left, Iterable<R> right, String localField, String foreignField, String targetField);

    <T> int count(NoSQLFilter filter, Class<T> clazz) throws IOException;

    <T> int count(NoSQLFilter filter, Class<T> clazz, int offset, int pageSize) throws IOException;

    <T> int countAll(Class<T> clazz) throws IOException;

    <T> int remove(T object) throws IOException;

    <T> int removeAll(Collection<T> objects) throws IOException;

    <T> int removeAll(NoSQLFilter filter, Class<T> clazz) throws IOException;

    F getFilter(NoSQLFilter filter);


}
