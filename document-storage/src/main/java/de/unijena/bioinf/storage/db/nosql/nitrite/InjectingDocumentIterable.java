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
import org.dizitart.no2.NitriteCollection;
import org.dizitart.no2.exceptions.InvalidOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Set;

import static org.dizitart.no2.exceptions.ErrorMessage.REMOVE_ON_DOCUMENT_ITERATOR_NOT_SUPPORTED;

public class InjectingDocumentIterable implements Iterable<Document> {

    private final Iterable<Document> parent;

    private final Set<String> injectedFields;

    private final NitriteCollection collection;


    public InjectingDocumentIterable(Iterable<Document> parent, Set<String> injectedFields, NitriteCollection collection) {
        this.parent = parent;
        this.injectedFields = injectedFields;
        this.collection = collection;
    }

    @NotNull
    @Override
    public Iterator<Document> iterator() {
        return new InjectingDocumentIterator();
    }

    private class InjectingDocumentIterator implements Iterator<Document> {
        private final Iterator<Document> iterator;
        private Document nextElement = null;

        InjectingDocumentIterator() {
            iterator = parent.iterator();
            nextMatch();
        }

        @Override
        public boolean hasNext() {
            return nextElement != null;
        }

        @Override
        public Document next() {
            Document returnValue = nextElement;
            nextMatch();
            return returnValue;
        }

        private void nextMatch() {
            while (iterator.hasNext()) {
                Document document = iterator.next();
                if (document != null) {
                    Document injected = inject(new Document(document), injectedFields, collection);
                    if (injected != null) {
                        nextElement = injected;
                        return;
                    }
                } else {
                    nextElement = null;
                }
            }

            nextElement = null;
        }

        @Override
        public void remove() {
            throw new InvalidOperationException(REMOVE_ON_DOCUMENT_ITERATOR_NOT_SUPPORTED);
        }

    }

    public static Document inject(Document original, Set<String> injectedFields, NitriteCollection collection) {
        if (injectedFields.isEmpty()) return original;

        Document result = new Document(original);
        if (original.getId() == null) {
            throw new RuntimeException("Document has no ID");
        }
        Document fromDB = collection.getById(original.getId());
        if (fromDB == null) {
            throw new RuntimeException("No such document: " + original.getId());
        }
        for (String field : injectedFields) {
            result.put(field, fromDB.get(field));
        }
        return result;
    }

}
