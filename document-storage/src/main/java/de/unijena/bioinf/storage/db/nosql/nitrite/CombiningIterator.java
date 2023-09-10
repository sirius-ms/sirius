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
import org.dizitart.no2.mapper.NitriteMapper;

import java.util.Iterator;

public class CombiningIterator<T, I extends CombiningDocumentIterator> implements Iterator<T> {

    protected final I documentIterator;

    protected final Class<T> targetClass;

    protected final NitriteMapper mapper;

    public CombiningIterator(Class<T> targetClass, I documentIterator, NitriteMapper mapper) {
        this.documentIterator = documentIterator;
        this.targetClass = targetClass;
        this.mapper = mapper;
    }

    @Override
    public boolean hasNext() {
        return documentIterator.hasNext();
    }

    @Override
    public T next() {
        Document document = documentIterator.next();
        return mapper.asObject(document, targetClass);
    }
}