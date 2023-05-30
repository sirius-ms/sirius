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

import lombok.extern.slf4j.Slf4j;
import org.dizitart.no2.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class MergedDocumentIterator extends CombiningDocumentIterator {
    private final String[] targetFields;

    public MergedDocumentIterator(Iterable<Document> parents, Function<Object, Iterable<Document>> children, String localField, String... targetFields) {
        super(parents, children, localField);
        this.targetFields = targetFields;
        //todo maybe check and warn if selected localField is not
    }

    @Override
    public Document next() {
        Document targetDoc = new Document(parentIterator.next());
        Object localObject = targetDoc.get(localField);
        if (localObject == null) {
            return targetDoc;
        }

        List<Document> children = new ArrayList<>();
        for (Document foreignDoc : this.children.apply(localObject)) {
            children.add(foreignDoc);
            break;
        }

        if (!children.isEmpty()) {
            if (children.size() > 1)
                log.warn("Found more than one matching child for matched field '" + localField + "' but combining operation was was merge which only allows one2one. Merging the first matching child!");
            Document child = children.get(0);
            for (String targetField : targetFields)
                targetDoc.put(targetField, child.get(targetField));
        }

        return targetDoc;
    }
}
