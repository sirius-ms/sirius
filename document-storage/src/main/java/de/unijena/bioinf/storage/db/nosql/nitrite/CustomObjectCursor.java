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

import lombok.Getter;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.FindPlan;
import org.dizitart.no2.common.Lookup;
import org.dizitart.no2.common.RecordStream;
import org.dizitart.no2.common.mapper.NitriteMapper;
import org.dizitart.no2.common.util.DocumentUtils;
import org.dizitart.no2.common.util.ValidationUtils;
import org.dizitart.no2.exceptions.InvalidOperationException;
import org.dizitart.no2.exceptions.ValidationException;
import org.dizitart.no2.repository.Cursor;

import java.lang.reflect.Modifier;
import java.util.Iterator;

public class CustomObjectCursor<T> implements Cursor<T> {

    @Getter
    private final RecordStream<Document> recordStream;

    @Getter
    private final FindPlan findPlan;
    private final NitriteMapper nitriteMapper;

    @Getter
    private final Class<T> type;

    public CustomObjectCursor(NitriteMapper nitriteMapper, RecordStream<Document> recordStream, FindPlan findPlan, Class<T> type) {
        this.nitriteMapper = nitriteMapper;
        this.recordStream = recordStream;
        this.type = type;
        this.findPlan = findPlan;
    }

    public long size() {
        return this.recordStream.size();
    }

    @Override
    public <P> RecordStream<P> project(Class<P> aClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <Foreign, Joined> RecordStream<Joined> join(Cursor<Foreign> cursor, Lookup lookup, Class<Joined> aClass) {
        throw new UnsupportedOperationException();
    }

    public Iterator<T> iterator() {
        return new ObjectCursorIterator(this.recordStream.iterator());
    }

    private <D> Document emptyDocument(NitriteMapper nitriteMapper, Class<D> type) {
        if (type.isPrimitive()) {
            throw new ValidationException("Cannot project to primitive type");
        } else if (type.isInterface()) {
            throw new ValidationException("Cannot project to interface");
        } else if (type.isArray()) {
            throw new ValidationException("Cannot project to array");
        } else if (Modifier.isAbstract(type.getModifiers())) {
            throw new ValidationException("Cannot project to abstract type");
        } else {
            ValidationUtils.validateProjectionType(type, nitriteMapper);
            Document dummyDoc = DocumentUtils.skeletonDocument(nitriteMapper, type);
            if (dummyDoc != null && dummyDoc.size() != 0) {
                return dummyDoc;
            } else {
                throw new ValidationException("Cannot project to empty type");
            }
        }
    }

    private class ObjectCursorIterator implements Iterator<T> {
        private final Iterator<Document> documentIterator;

        ObjectCursorIterator(Iterator<Document> documentIterator) {
            this.documentIterator = documentIterator;
        }

        public boolean hasNext() {
            return this.documentIterator.hasNext();
        }

        @SuppressWarnings("unchecked")
        public T next() {
            Document document = this.documentIterator.next();
            return (T) CustomObjectCursor.this.nitriteMapper.tryConvert(document, CustomObjectCursor.this.type);
        }

        public void remove() {
            throw new InvalidOperationException("Remove on a cursor is not supported");
        }
    }

}
