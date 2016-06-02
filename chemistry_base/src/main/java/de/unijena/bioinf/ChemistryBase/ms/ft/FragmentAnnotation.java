/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.ChemistryBase.ms.ft;

public final class FragmentAnnotation<T> {

    protected final int id;
    protected final Class<T> klass;
    int capa;
    FragmentAnnotation<? extends T> alias;

    FragmentAnnotation(int id, int capa, Class<T> klass) {
        this.id = id;
        this.klass = klass;
        this.capa = capa;
        this.alias = null;
    }

    <S extends T> FragmentAnnotation(FragmentAnnotation<S> prev, Class<T> newOne) {
        this.id = prev.id;
        this.klass = newOne;
        this.capa = prev.capa;
        this.alias = prev;
    }

    public T get(Fragment vertex) {
        return (T) (vertex.getAnnotation(id));
    }

    public Class<T> getAnnotationType() {
        return klass;
    }

    public boolean isAlias() {
        return alias!=null;
    }

    public Class<? extends T> getAliasType() {
        return alias.getAnnotationType();
    }

    public FragmentAnnotation<? extends T> getAlias() {
        return alias;
    }

    public T getOrCreate(Fragment vertex) {
        final T obj = get(vertex);
        if (obj == null) {
            try {
                final T newObj = (alias != null ? alias.getAnnotationType() : klass).newInstance();
                vertex.setAnnotation(id, capa, newObj);
                return newObj;
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else return obj;
    }

    public void set(Fragment vertex, T obj) {
        if (alias != null) throw new UnsupportedOperationException("Cannot set values of alias annotations for alias. Use '" + alias.getAnnotationType().getSimpleName() + "' instead of '" + klass.getSimpleName() + "'");
        vertex.setAnnotation(id, capa, obj);
    }

}
