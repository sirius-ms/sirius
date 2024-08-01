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

import de.unijena.bioinf.storage.db.nosql.nitrite.joining.JoinedDocumentStream;
import de.unijena.bioinf.storage.db.nosql.nitrite.projection.InjectedDocumentStream;
import de.unijena.bioinf.storage.db.nosql.nitrite.projection.OptFieldDocumentStream;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.collection.NitriteId;
import org.dizitart.no2.common.RecordStream;
import org.dizitart.no2.common.processors.ProcessorChain;
import org.dizitart.no2.common.streams.DocumentStream;
import org.dizitart.no2.common.tuples.Pair;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

public class CustomDocumentStream extends DocumentStream {

    private final RecordStream<Pair<NitriteId, Document>> recordStream;

    private final ProcessorChain processorChain;

    private CustomDocumentStream(RecordStream<Pair<NitriteId, Document>> recordStream, ProcessorChain processorChain) {
        super(recordStream, processorChain);
        this.recordStream = recordStream;
        this.processorChain = processorChain;
    }

    public RecordStream<Document> project(Set<String> omittedFields) {
        if (omittedFields.isEmpty())
            return this;
        return new OptFieldDocumentStream(this.recordStream, this.processorChain, omittedFields);
    }

    public RecordStream<Document> inject(Set<String> injectedFields, NitriteCollection collection) {
        if (injectedFields.isEmpty())
            return this;
        return new InjectedDocumentStream(this.recordStream, this.processorChain, injectedFields, collection);
    }

    public RecordStream<Document> join(Function<Object, Iterable<Document>> children, String localField, String targetField) {
        return new JoinedDocumentStream(this.recordStream, this.processorChain, children, localField, targetField);
    }

    @SuppressWarnings("unchecked")
    public static CustomDocumentStream of(Iterable<Document> iterable) {
        if (iterable instanceof DocumentStream documentStream) {
            try {
                Field chainField = DocumentStream.class.getDeclaredField("processorChain");
                chainField.setAccessible(true);
                ProcessorChain chain = (ProcessorChain) chainField.get(documentStream);

                Field recordField = DocumentStream.class.getDeclaredField("recordStream");
                recordField.setAccessible(true);
                RecordStream<Pair<NitriteId, Document>> record = (RecordStream<Pair<NitriteId, Document>>) recordField.get(documentStream);

                return new CustomDocumentStream(record, chain);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            RecordStream<Pair<NitriteId, Document>> record = new RecordStream<>() {
                @NotNull
                @Override
                public Iterator<Pair<NitriteId, Document>> iterator() {
                    Iterator<Document> it = iterable.iterator();
                    return new Iterator<>() {
                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public Pair<NitriteId, Document> next() {
                            Document next = it.next();
                            return new Pair<>(next.getId(), next);
                        }
                    };
                }
            };

            return new CustomDocumentStream(record, new ProcessorChain());
        }
    }

}
