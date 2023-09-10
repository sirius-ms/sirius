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
import org.dizitart.no2.KeyValuePair;
import org.dizitart.no2.exceptions.InvalidOperationException;

import java.util.Iterator;
import java.util.Set;

import static org.dizitart.no2.exceptions.ErrorMessage.REMOVE_ON_DOCUMENT_ITERATOR_NOT_SUPPORTED;

public class ProjectingDocumentIterable implements Iterable<Document> {

    private final Iterable<Document> parent;
    private final Set<String> omittedFields;

    public ProjectingDocumentIterable(Iterable<Document> parent, Set<String> omittedFields) {
        this.parent = parent;
        this.omittedFields = omittedFields;
    }

    public void withOptionalFields(Set<String> optionalFields) {
        this.omittedFields.removeAll(optionalFields);
    }

    @Override
    public Iterator<Document> iterator() {
        return new ProjectingDocumentIterator();
    }

    private class ProjectingDocumentIterator implements Iterator<Document> {
        private final Iterator<Document> iterator;
        private Document nextElement = null;

        ProjectingDocumentIterator() {
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
                    Document projected = project(new Document(document), omittedFields);
                    if (projected != null) {
                        nextElement = projected;
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

    public static Document project(Document original, Set<String> omittedFields) {
        if (omittedFields.size() == 0) return original;
        Document result = new Document(original);

        for (KeyValuePair keyValuePair : original) {
            if (omittedFields.contains(keyValuePair.getKey())) {
                result.remove(keyValuePair.getKey());
            }
        }
        return result;
    }

}
