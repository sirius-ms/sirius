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

import java.util.Iterator;
import java.util.function.Function;

abstract class CombiningDocumentIterator implements Iterator<Document> {

    protected Iterator<Document> parentIterator;

    protected final Function<Object, Iterable<Document>> children;

    protected final String localField;

    public CombiningDocumentIterator(Iterable<Document> parents, Function<Object, Iterable<Document>> children, String localField) {
        this.parentIterator = parents.iterator();
        this.children = children;
        this.localField = localField;
    }

    /**
     * Just a default Override for more complex handling
     * @return true if another element is available
     */
    @Override
    public boolean hasNext() {
        return parentIterator.hasNext();
    }
}
