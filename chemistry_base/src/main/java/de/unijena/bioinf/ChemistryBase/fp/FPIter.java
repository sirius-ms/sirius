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

package de.unijena.bioinf.ChemistryBase.fp;

import java.util.Iterator;

public abstract class FPIter implements Iterable<FPIter>, Iterator<FPIter> {

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public Iterator<FPIter> iterator() {
        return clone();
    }

    public Iterator<Integer> asIndexIterator() {
        return new Iterator<Integer>() {
            @Override
            public boolean hasNext() {
                return FPIter.this.hasNext();
            }

            @Override
            public void remove() {
                FPIter.this.remove();
            }

            @Override
            public Integer next() {
                FPIter.this.next();
                return getIndex();
            }
        };
    }
    public Iterator<Double> asProbabilityIterator() {
        return new Iterator<Double>() {
            @Override
            public boolean hasNext() {
                return FPIter.this.hasNext();
            }

            @Override
            public void remove() {
                FPIter.this.remove();
            }

            @Override
            public Double next() {
                FPIter.this.next();
                return getProbability();
            }
        };
    }
    public Iterator<Boolean> asValueIterator() {
        return new Iterator<Boolean>() {
            @Override
            public boolean hasNext() {
                return FPIter.this.hasNext();
            }

            @Override
            public void remove() {
                FPIter.this.remove();
            }

            @Override
            public Boolean next() {
                FPIter.this.next();
                return isSet();
            }
        };
    }

    public Iterator<MolecularProperty> asMolecularPropertyIterator() {
        return new Iterator<MolecularProperty>() {
            @Override
            public boolean hasNext() {
                return FPIter.this.hasNext();
            }

            @Override
            public void remove() {
                FPIter.this.remove();
            }

            @Override
            public MolecularProperty next() {
                FPIter.this.next();
                return getMolecularProperty();
            }
        };
    }

    public double getProbability() {
        return isSet() ? 1 : 0;
    }
    public abstract boolean isSet();
    public abstract int getIndex();
    public abstract MolecularProperty getMolecularProperty();

    /**
     * Return a new iterator which points at index or on the first
     * index which is not larger than index.
     */
    public abstract FPIter jumpTo(int index);

    public abstract FPIter clone();

}
