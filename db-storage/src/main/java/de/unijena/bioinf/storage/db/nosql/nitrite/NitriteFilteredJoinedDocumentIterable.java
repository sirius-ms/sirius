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

import de.unijena.bioinf.storage.db.nosql.Filter;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongConsumer;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.dizitart.no2.Document;
import org.dizitart.no2.Lookup;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.RecordIterable;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.StreamSupport;

import static org.dizitart.no2.Document.createDocument;

public class NitriteFilteredJoinedDocumentIterable extends NitriteJoinedDocumentIterable {

    protected final Filter childFilter;

    public static NitriteFilteredJoinedDocumentIterable newInstance(String parentCollection, String childCollection, Filter childFilter, Iterable<Document> parents, String foreignField, String targetField, NitriteDatabase database) throws IOException {
        NitriteFilteredJoinedDocumentIterable iterable = new NitriteFilteredJoinedDocumentIterable(parentCollection, childCollection, childFilter, foreignField, targetField, database);
        iterable.initParentIds(parents);

        org.dizitart.no2.Cursor docCursor = (org.dizitart.no2.Cursor) database.find(childCollection, childFilter);
        Document projection = createDocument(foreignField, null);
        RecordIterable<Document> documents = docCursor.project(projection);
        LongSet pIds = new LongArraySet(documents.size());
        StreamSupport.stream(documents.spliterator(), false).mapToLong(doc -> (Long) doc.get(foreignField)).forEach((LongConsumer) pIds::add);

        iterable.parents.retainAll(pIds);

        return iterable;
    }

    protected NitriteFilteredJoinedDocumentIterable(String parentCollection, String childCollection, Filter childFilter, String foreignField, String targetField, NitriteDatabase database) {
        super(parentCollection, childCollection, foreignField, targetField, database);
        this.childFilter = childFilter;
    }

    @NotNull
    @Override
    public Iterator<Document> iterator() {
        if (this.parents.isEmpty()) {
            return Collections.emptyIterator();
        }
        return new FilteredJoinedDocumentIterator(parentCollection, childCollection, childFilter, parents, foreignField, targetField, database);
    }

    protected static class FilteredJoinedDocumentIterator extends JoinedDocumentIterator {

        private final Filter childFilter;
        private Document next;

        public FilteredJoinedDocumentIterator(String parentCollection, String childCollection, Filter childFilter, LongSet parents, String foreignField, String targetField, NitriteDatabase database) {
            super(parentCollection, childCollection, parents, foreignField, targetField, database);
            this.childFilter = childFilter;
            this.next = null;
        }

        @Override
        public boolean hasNext() {
            if (next == null) {
                next = advance();
            }
            return next != null;
        }

        @Override
        public Document next() {
            if (next == null) {
                next = advance();
            }
            Document result = next;
            next = null;
            return result;
        }

        public Document advance() {
            try {
                Document result = null;
                while (result == null && parents.hasNext()) {
                    long id = parents.nextLong();
                    Iterable<Document> parent = database.find(parentCollection, new Filter().eq("_id", id));
                    Filter cFilter = new Filter().and().or().eq(foreignField, id).eq(foreignField, NitriteId.createId(id)).end();
                    cFilter.filterChain.addAll(childFilter.filterChain);
                    org.dizitart.no2.Cursor children = (org.dizitart.no2.Cursor) database.find(childCollection, cFilter);
                    if (children.size() == 0) {
                        continue;
                    }

                    Lookup lookup = new Lookup();
                    lookup.setLocalField("_id");
                    lookup.setForeignField(foreignField);
                    lookup.setTargetField(targetField);

                    RecordIterable<Document> res = ((org.dizitart.no2.Cursor) parent).join(children, lookup);
                    result = res.firstOrDefault();
                }
                return result;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
