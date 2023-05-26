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

import org.dizitart.no2.Cursor;
import org.dizitart.no2.Document;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

public class NitriteJoinedDocumentIterable implements Iterable<Document> {

    protected Cursor parents;

    protected Function<Object, Cursor> children;

    protected final String localField;

    protected final String targetField;


    protected NitriteJoinedDocumentIterable(
            Iterable<Document> parents,
            Function<Object, Cursor> children,
            String localField,
            String targetField
    ) throws IOException {
        if (!(parents instanceof Cursor)) {
            throw new IOException("parents must be cursor!");
        }
        this.parents = (Cursor) parents;
        this.children = children;
        this.localField = localField;
        this.targetField = targetField;
    }

    @NotNull
    @Override
    public Iterator<Document> iterator() {
        if (parents.size() == 0) {
            return Collections.emptyIterator();
        }
        return new JoinedDocumentIterator(parents);
    }

    protected class JoinedDocumentIterator implements Iterator<Document> {

        protected Iterator<Document> parentIterator;

        public JoinedDocumentIterator(Cursor parents) {
            this.parentIterator = parents.iterator();
        }

        @Override
        public boolean hasNext() {
            return parentIterator.hasNext();
        }

        @Override
        public Document next() {
            Document targetDoc = new Document(parentIterator.next());
            Object localObject = targetDoc.get(localField);
            if (localObject == null) {
                return targetDoc;
            }

            Set<Document> target = new HashSet<>();
            org.dizitart.no2.Cursor childCursor = children.apply(localObject);
            for (Document foreignDoc : childCursor) {
                target.add(foreignDoc);
            }
            if (!target.isEmpty()) {
                targetDoc.put(targetField, target);
            }

            return targetDoc;
        }

    }

}
