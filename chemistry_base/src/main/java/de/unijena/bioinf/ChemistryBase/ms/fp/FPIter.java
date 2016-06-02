package de.unijena.bioinf.ChemistryBase.ms.fp;

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

    public abstract FPIter clone();

}
