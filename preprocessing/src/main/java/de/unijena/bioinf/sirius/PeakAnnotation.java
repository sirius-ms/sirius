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
package de.unijena.bioinf.sirius;

public final class PeakAnnotation<T> {

    private final int id;
    private final Class<T> klass;

    PeakAnnotation(int id, Class<T> klass) {
        this.id = id;
        this.klass = klass;
    }

    public T get(ProcessedPeak peak) {
        return (T)(peak.getAnnotation(id));
    }

    public T getOrCreate(ProcessedPeak peak) {
        final T obj = get(peak);
        if (obj == null) {
            try {
                final T newObj = klass.newInstance();
                peak.setAnnotation(id,  newObj);
                return newObj;
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else return obj;
    }

    public void set(ProcessedPeak peak, T obj) {
        peak.setAnnotation(id,  obj);
    }

}
