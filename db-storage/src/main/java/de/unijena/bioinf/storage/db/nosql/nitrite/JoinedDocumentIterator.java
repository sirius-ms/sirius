/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.storage.db.nosql.nitrite;

import org.dizitart.no2.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class JoinedDocumentIterator extends CombiningDocumentIterator {
    protected final String targetField;

    public JoinedDocumentIterator(Iterable<Document> parents, Function<Object, Iterable<Document>> children, String localField, String targetField) {
        super(parents, children, localField);
        this.targetField = targetField;
    }

    @Override
    public Document next() {
        Document targetDoc = new Document(parentIterator.next());
        Object localObject = targetDoc.get(localField);
        if (localObject == null) {
            return targetDoc;
        }

        List<Document> target = new ArrayList<>();
        for (Document foreignDoc : children.apply(localObject)) {
            target.add(foreignDoc);
        }
        if (!target.isEmpty()) {
            targetDoc.put(targetField, target);
        }

        return targetDoc;
    }

}