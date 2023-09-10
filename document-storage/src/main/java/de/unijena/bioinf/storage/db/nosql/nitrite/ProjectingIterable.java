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

import org.dizitart.no2.Document;
import org.dizitart.no2.mapper.NitriteMapper;
import org.dizitart.no2.objects.Cursor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;

public class ProjectingIterable<T> implements Iterable<T> {

    private final ProjectingDocumentIterable documentIterable;

    private final Class<T> targetClass;

    private final NitriteMapper mapper;

    public ProjectingIterable(Class<T> targetClass, Cursor<T> cursor, Set<String> omittedFields, NitriteMapper mapper) throws IOException {
        try {
            Field cField = cursor.getClass().getDeclaredField("cursor");
            cField.setAccessible(true);
            this.documentIterable = new ProjectingDocumentIterable((org.dizitart.no2.Cursor) cField.get(cursor), omittedFields);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IOException(e);
        }
        this.targetClass = targetClass;
        this.mapper = mapper;
    }

    public void withOptionalFields(Set<String> optionalFields) {
        documentIterable.withOptionalFields(optionalFields);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new ProjectingIterator();
    }

    private class ProjectingIterator implements Iterator<T> {

        private final Iterator<Document> documentIterator;

        private ProjectingIterator() {
            this.documentIterator = documentIterable.iterator();
        }

        @Override
        public boolean hasNext() {
            return documentIterator.hasNext();
        }

        @Override
        public T next() {
            Document document = documentIterator.next();
            return mapper.asObject(document, targetClass);
        }

    }

}
