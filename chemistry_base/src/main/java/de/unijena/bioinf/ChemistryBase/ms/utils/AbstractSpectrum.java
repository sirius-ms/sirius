
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

package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class AbstractSpectrum<T extends Peak> implements Spectrum<T> {

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private int index;

            @Override
            public boolean hasNext() {
                return index < size();
            }

            @Override
            public T next() {
                if (index < size())
                    return getPeakAt(index++);
                else
                    throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    public double getMzAt(int index) {
        return getPeakAt(index).getMass();
    }

    public double getIntensityAt(int index) {
        return getPeakAt(index).getIntensity();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj.getClass().equals(getClass()))) return false;
        AbstractSpectrum<?> spectrum = (AbstractSpectrum<?>) obj;
        if (spectrum.size() != size()) return false;
        for (int i = 0; i < size(); i++) {
            if (!spectrum.getPeakAt(i).equals(getPeakAt(i)))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (int i = 0; i < size(); i++) {
            hash = hash ^ getPeakAt(i).hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        if (size() == 0) return "{}";
        final StringBuilder buffer = new StringBuilder(size() * 12);
        final Iterator<T> iter = iterator();
        buffer.append("{").append(iter.next());
        while (iter.hasNext()) {
            buffer.append(", ");
            buffer.append(iter.next());
        }
        buffer.append("}");
        return buffer.toString();
    }

}
