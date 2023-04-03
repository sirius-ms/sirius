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
import org.dizitart.no2.Lookup;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.RecordIterable;
import org.dizitart.no2.objects.Cursor;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class NitriteJoinedIterable<T, P, C> implements Iterable<T> {

    private final Class<T> clazz;
    private final Class<P> parentClass;
    private final Class<C> childClass;
    private final Set<Long> parents;
    private final String foreignField;
    private final String targetField;

    private final NitriteDatabase database;

    public NitriteJoinedIterable(Class<T> clazz, Class<P> parentClass, Class<C> childClass, Cursor<P> parents, String foreignField, String targetField, NitriteDatabase database) {
        this.clazz = clazz;
        this.parentClass = parentClass;
        this.childClass = childClass;
        this.parents = parents.idSet().stream().mapToLong(NitriteId::getIdValue).boxed().collect(Collectors.toSet());
        this.foreignField = foreignField;
        this.targetField = targetField;
        this.database = database;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        if (parents.isEmpty()) {
            return Collections.emptyIterator();
        }
        return new JoinedIterator(clazz, parentClass, childClass, parents, foreignField, targetField, database);
    }

    private class JoinedIterator implements Iterator<T> {

        private final Class<T> clazz;
        private final Class<P> parentClass;
        private final Class<C> childClass;
        private final Iterator<Long> parents;
        private final String foreignField;
        private final String targetField;
        private final NitriteDatabase database;

        public JoinedIterator(Class<T> clazz, Class<P> parentClass, Class<C> childClass, Set<Long> parents, String foreignField, String targetField, NitriteDatabase database) {
            this.clazz = clazz;
            this.parentClass = parentClass;
            this.childClass = childClass;
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
        public T next() {
            try {
                long id = parents.next();
                Iterable<P> parent = database.find(new Filter().eq("id", id), parentClass);
                Iterable<C> children = database.find(new Filter().eq(foreignField, id), childClass);

                Lookup lookup = new Lookup();
                lookup.setLocalField("id");
                lookup.setForeignField(foreignField);
                lookup.setTargetField(targetField);

                RecordIterable<T> res = ((org.dizitart.no2.objects.Cursor<P>) parent).join((org.dizitart.no2.objects.Cursor<C>) children, lookup, clazz);
                return res.firstOrDefault();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
