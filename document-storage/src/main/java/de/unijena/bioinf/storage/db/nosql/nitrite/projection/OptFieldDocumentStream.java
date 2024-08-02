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

package de.unijena.bioinf.storage.db.nosql.nitrite.projection;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteId;
import org.dizitart.no2.common.RecordStream;
import org.dizitart.no2.common.processors.ProcessorChain;
import org.dizitart.no2.common.tuples.Pair;
import org.dizitart.no2.exceptions.InvalidOperationException;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public class OptFieldDocumentStream implements RecordStream<Document> {

    private final RecordStream<Pair<NitriteId, Document>> recordStream;
    private final ProcessorChain processorChain;

    private final Set<String> omittedFields;

    public OptFieldDocumentStream(RecordStream<Pair<NitriteId, Document>> recordStream, ProcessorChain processorChain, Set<String> omittedFields) {
        this.recordStream = recordStream;
        this.processorChain = processorChain;
        this.omittedFields = omittedFields;
    }

    @Override
    public Iterator<Document> iterator() {
        Iterator<Pair<NitriteId, Document>> iterator = this.recordStream == null ? Collections.emptyIterator() : this.recordStream.iterator();
        return new OptFieldDocumentStream.ProjectedDocumentIterator(iterator, this.processorChain, this.omittedFields);
    }

    public String toString() {
        return this.toList().toString();
    }

    private static class ProjectedDocumentIterator implements Iterator<Document> {
        private final Iterator<Pair<NitriteId, Document>> iterator;
        private final ProcessorChain processorChain;
        private Document nextElement = null;
        private final Set<String> omittedFields;

        ProjectedDocumentIterator(Iterator<Pair<NitriteId, Document>> iterator, ProcessorChain processorChain, Set<String> omittedFields) {
            this.iterator = iterator;
            this.processorChain = processorChain;
            this.omittedFields = omittedFields;
            this.nextMatch();
        }

        public boolean hasNext() {
            return this.nextElement != null;
        }

        public Document next() {
            Document returnValue = this.nextElement.clone();
            this.nextMatch();
            return returnValue;
        }

        private void nextMatch() {
            while(true) {
                if (this.iterator.hasNext()) {
                    Pair<NitriteId, Document> next = this.iterator.next();
                    Document document = next.getSecond();
                    if (document == null) {
                        continue;
                    }

                    Document projected = project(document, this.omittedFields, this.processorChain);
                    if (projected == null) {
                        continue;
                    }

                    this.nextElement = projected;
                    return;
                }

                this.nextElement = null;
                return;
            }
        }

        public void remove() {
            throw new InvalidOperationException("Remove on a cursor is not supported");
        }

    }

    public static Document project(Document original, Set<String> omittedFields, ProcessorChain processorChain) {
        if (omittedFields.isEmpty()) {
            return original;
        } else {
            Document newDoc = Document.createDocument();

            for (Pair<String, Object> pair : original) {
                if (!omittedFields.contains(pair.getFirst())) {
                    newDoc.put(pair.getFirst(), pair.getSecond());
                }
            }

            newDoc = processorChain.processAfterRead(newDoc);
            return newDoc;
        }
    }

}
