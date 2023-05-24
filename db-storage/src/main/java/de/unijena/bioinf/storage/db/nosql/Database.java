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

public interface Database<DocType> extends Closeable, AutoCloseable {

    enum SortOrder {
        ASCENDING, DESCENDING
    }

    // CRUD operations

    <T> int insert(T object) throws IOException;

    <T> int insertAll(Iterable<T> objects) throws IOException;

    int insert(String collectionName, DocType document) throws IOException;

    int insertAll(String collectionName, Iterable<DocType> documents) throws IOException;

    <T> int upsert(T object) throws IOException;

    <T> int upsertAll(Iterable<T> objects) throws IOException;

    int upsert(String collectionName, DocType document) throws IOException;

    int upsertAll(String collectionName, Iterable<DocType> documents) throws IOException;

    <T> T getById(long id, Class<T> clazz) throws IOException;

    DocType getById(String collectionName, long id) throws IOException;

    <T> Iterable<T> find(Filter filter, Class<T> clazz) throws IOException;

    <T> Iterable<T> find(Filter filter, Class<T> clazz, int offset, int pageSize) throws IOException;

    <T> Iterable<T> find(Filter filter, Class<T> clazz, String sortField, SortOrder sortOrder) throws IOException;

    <T> Iterable<T> find(Filter filter, Class<T> clazz, int offset, int pageSize, String sortField, SortOrder sortOrder) throws IOException;

    Iterable<DocType> find(String collectionName, Filter filter) throws IOException;

    Iterable<DocType> find(String collectionName, Filter filter, int offset, int pageSize) throws IOException;

    Iterable<DocType> find(String collectionName, Filter filter, String sortField, SortOrder sortOrder) throws IOException;

    Iterable<DocType> find(String collectionName, Filter filter, int offset, int pageSize, String sortField, SortOrder sortOrder) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, int offset, int pageSize) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, String sortField, SortOrder sortOrder) throws IOException;

    <T> Iterable<T> findAll(Class<T> clazz, int offset, int pageSize, String sortField, SortOrder sortOrder) throws IOException;

    Iterable<DocType> findAll(String collectionName) throws IOException;

    Iterable<DocType> findAll(String collectionName, int offset, int pageSize) throws IOException;

    Iterable<DocType> findAll(String collectionName, String sortField, SortOrder sortOrder) throws IOException;

    Iterable<DocType> findAll(String collectionName, int offset, int pageSize, String sortField, SortOrder sortOrder) throws IOException;

    <T, P, C> Iterable<T> joinAllChildren(Class<T> clazz, Class<P> parentClass, Class<C> childClass, Iterable<P> parents, String foreignField, String targetField);

    <T, P, C> Iterable<T> joinChildren(Class<T> clazz, Class<P> parentClass, Class<C> childClass, Filter childFilter, Iterable<P> parents, String foreignField, String targetField) throws IOException;

    Iterable<DocType> joinAllChildren(String parentCollectionName, String childCollectionName, Iterable<DocType> parents, String foreignField, String targetField);

    Iterable<DocType> joinChildren(String parentCollectionName, String childCollectionName, Filter childFilter, Iterable<DocType> parents, String foreignField, String targetField) throws IOException;

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

    <ObjectFilterType> ObjectFilterType getObjectFilter(Filter filter);

    <FilterType> FilterType getFilter(Filter filter);

}
