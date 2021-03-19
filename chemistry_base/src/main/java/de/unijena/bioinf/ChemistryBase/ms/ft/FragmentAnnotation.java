
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ms.annotations.DataAnnotation;

import java.util.function.Supplier;

public final class FragmentAnnotation<T extends DataAnnotation> {

    protected final int id;
    protected final Class<T> klass;
    protected Supplier<T> nullElement;
    int capa;

    FragmentAnnotation(int id, int capa, Class<T> klass, Supplier<T> nullElement) {
        this.id = id;
        this.klass = klass;
        this.capa = capa;
        this.nullElement = nullElement;
    }

    public T getNullElement() {
        return nullElement.get();
    }

    public T get(Fragment vertex, Supplier<T> defaultValue) {
        final T val = (T) (vertex.getAnnotation(id));
        if (val==null) return defaultValue.get();
        else return val;
    }

    public T get(Fragment vertex) {
        final T val = (T) (vertex.getAnnotation(id));
        if (val==null && nullElement!=null) {
            T o = nullElement.get();
            vertex.setAnnotation(id, capa, o);
            return o;
        } else return val;
    }

    public Class<T> getAnnotationType() {
        return klass;
    }

    public void set(Fragment vertex, T obj) {
        vertex.setAnnotation(id, capa, obj);
    }

}
