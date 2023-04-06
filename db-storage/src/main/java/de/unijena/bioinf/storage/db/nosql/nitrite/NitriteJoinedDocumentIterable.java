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
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.dizitart.no2.Document;
import org.dizitart.no2.Lookup;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.RecordIterable;
import org.dizitart.no2.objects.Cursor;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.StreamSupport;

public class NitriteJoinedDocumentIterable implements Iterable<Document> {
    protected final String parentCollection;
    protected final String childCollection;
    protected LongSet parents;
    protected final String foreignField;
    protected final String targetField;

    protected final NitriteDatabase database;

    public static NitriteJoinedDocumentIterable newInstance(String parentCollection, String childCollection, Iterable<Document> parents, String foreignField, String targetField, NitriteDatabase database) {
        NitriteJoinedDocumentIterable iterable = new NitriteJoinedDocumentIterable(parentCollection, childCollection, foreignField, targetField, database);
        iterable.initParentIds(parents);
        return iterable;
    }

    protected NitriteJoinedDocumentIterable(String parentCollection, String childCollection, String foreignField, String targetField, NitriteDatabase database) {
        this.parentCollection = parentCollection;
        this.childCollection = childCollection;
        this.foreignField = foreignField;
        this.targetField = targetField;
        this.database = database;
    }

    protected void initParentIds(Iterable<Document> parents) {
        if (parents instanceof Cursor) {
            this.parents = new LongArraySet(((Cursor<Document>) parents).size());
            ((Cursor<Document>) parents).idSet().stream().mapToLong(NitriteId::getIdValue).forEach((LongConsumer) this.parents::add);
        } else if (parents instanceof org.dizitart.no2.Cursor) {
            this.parents = new LongArraySet(((org.dizitart.no2.Cursor) parents).size());
            ((org.dizitart.no2.Cursor) parents).idSet().stream().mapToLong(NitriteId::getIdValue).forEach((LongConsumer) this.parents::add);
        } else {
            this.parents = new LongArraySet();
            StreamSupport.stream(parents.spliterator(), false).mapToLong(p -> (long) p.get("_id")).forEach((LongConsumer) this.parents::add);
        }
    }

    @NotNull
    @Override
    public Iterator<Document> iterator() {
        if (parents.isEmpty()) {
            return Collections.emptyIterator();
        }
        return new JoinedDocumentIterator(parentCollection, childCollection, parents, foreignField, targetField, database);
    }

    protected static class JoinedDocumentIterator implements Iterator<Document> {

        protected final String parentCollection;
        protected final String childCollection;
        protected LongIterator parents;
        protected final String foreignField;
        protected final String targetField;
        protected final NitriteDatabase database;

        public JoinedDocumentIterator(String parentCollection, String childCollection, LongSet parents, String foreignField, String targetField, NitriteDatabase database) {
            this.parentCollection = parentCollection;
            this.childCollection = childCollection;
            this.parents = parents.iterator();
            this.foreignField = foreignField;
            this.targetField = targetField;
            this.database = database;
        }

        @Override
        public boolean hasNext() {
            return parents.hasNext();
        }

        @Override
        public Document next() {
            try {
                long id = parents.nextLong();
                Iterable<Document> parent = database.find(parentCollection, new Filter().eq("_id", id));
                Iterable<Document> children = database.find(childCollection, new Filter().or().eq(foreignField, id).eq(foreignField, NitriteId.createId(id)));

                Lookup lookup = new Lookup();
                lookup.setLocalField("_id");
                lookup.setForeignField(foreignField);
                lookup.setTargetField(targetField);

                RecordIterable<Document> res = ((org.dizitart.no2.Cursor) parent).join((org.dizitart.no2.Cursor) children, lookup);
                return res.firstOrDefault();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
