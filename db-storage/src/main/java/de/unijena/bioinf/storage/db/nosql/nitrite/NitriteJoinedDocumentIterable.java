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

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

public class NitriteJoinedDocumentIterable implements Iterable<Document> {

    protected final Iterable<Document> parents;

    protected final Function<Object, Iterable<Document>> children;

    protected final String localField;

    protected final String targetField;


    protected NitriteJoinedDocumentIterable(
            Iterable<Document> parents,
            Function<Object, Iterable<Document>> children,
            String localField,
            String targetField
    ) {
        this.parents = parents;
        this.children = children;
        this.localField = localField;
        this.targetField = targetField;
    }

    @NotNull
    @Override
    public Iterator<Document> iterator() {
        if (!parents.iterator().hasNext()) {
            return Collections.emptyIterator();
        }
        return new JoinedDocumentIterator(parents, children, localField, targetField);
    }

    static class JoinedDocumentIterator implements Iterator<Document> {

        protected Iterator<Document> parentIterator;

        protected final Function<Object, Iterable<Document>> children;

        protected final String localField;

        protected final String targetField;

        public JoinedDocumentIterator(Iterable<Document> parents, Function<Object, Iterable<Document>> children, String localField, String targetField) {
            this.parentIterator = parents.iterator();
            this.children = children;
            this.localField = localField;
            this.targetField = targetField;
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
            for (Document foreignDoc : children.apply(localObject)) {
                target.add(foreignDoc);
            }
            if (!target.isEmpty()) {
                targetDoc.put(targetField, target);
            }

            return targetDoc;
        }

    }

}
