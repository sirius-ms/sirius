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

package de.unijena.bioinf.storage.db.nosql.nitrite.joining;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteId;
import org.dizitart.no2.common.RecordStream;
import org.dizitart.no2.common.processors.ProcessorChain;
import org.dizitart.no2.common.tuples.Pair;
import org.dizitart.no2.exceptions.InvalidOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class JoinedDocumentStream implements RecordStream<Document> {

    private final RecordStream<Pair<NitriteId, Document>> recordStream;
    private final ProcessorChain processorChain;

    private final Function<Object, Iterable<Document>> children;

    private final String localField;

    private final String targetField;

    public JoinedDocumentStream(RecordStream<Pair<NitriteId, Document>> recordStream, ProcessorChain processorChain, Function<Object, Iterable<Document>> children, String localField, String targetField) {
        this.recordStream = recordStream;
        this.processorChain = processorChain;
        this.children = children;
        this.localField = localField;
        this.targetField = targetField;
    }


    @NotNull
    @Override
    public Iterator<Document> iterator() {
        Iterator<Pair<NitriteId, Document>> iterator = this.recordStream == null ? Collections.emptyIterator() : this.recordStream.iterator();
        return new JoinedDocumentStream.JoinedDocumentIterator(iterator, this.processorChain, this.children, this.localField, this.targetField);
    }

    private static class JoinedDocumentIterator implements Iterator<Document> {

        private final Iterator<Pair<NitriteId, Document>> iterator;
        private final ProcessorChain processorChain;
        private Document nextElement = null;

        private final Function<Object, Iterable<Document>> children;

        private final String localField;

        private final String targetField;

        JoinedDocumentIterator(Iterator<Pair<NitriteId, Document>> iterator, ProcessorChain processorChain, Function<Object, Iterable<Document>> children, String localField, String targetField) {
            this.iterator = iterator;
            this.processorChain = processorChain;
            this.children = children;
            this.localField = localField;
            this.targetField = targetField;
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

                    Document targetDoc = Document.createDocument();
                    for (Pair<String, Object> pair : document) {
                        targetDoc.put(pair.getFirst(), pair.getSecond());
                    }
                    Object localObject = targetDoc.get(localField);
                    if (localObject == null) {
                        this.nextElement = targetDoc;
                        return;
                    }

                    List<Document> target = new ArrayList<>();
                    for (Document foreignDoc : children.apply(localObject)) {
                        target.add(foreignDoc);
                    }
                    if (!target.isEmpty()) {
                        targetDoc.put(targetField, target);
                    }

                    targetDoc = processorChain.processAfterRead(targetDoc);

                    this.nextElement = targetDoc;
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

}
