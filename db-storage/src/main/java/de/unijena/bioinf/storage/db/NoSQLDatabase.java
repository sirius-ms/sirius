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

    public <T, P, C> Iterable<T> joinChildren(Class<T> clazz, Class<P> parentClass, Class<C> childClass, Iterable<P> parents, String foreignField, String targetField);

    <T> int count(NoSQLFilter filter, Class<T> clazz) throws IOException;

    <T> int count(NoSQLFilter filter, Class<T> clazz, int offset, int pageSize) throws IOException;

    <T> int countAll(Class<T> clazz) throws IOException;

    <T> int remove(T object) throws IOException;

    <T> int removeAll(Collection<T> objects) throws IOException;

    <T> int removeAll(NoSQLFilter filter, Class<T> clazz) throws IOException;

    F getFilter(NoSQLFilter filter);


}
