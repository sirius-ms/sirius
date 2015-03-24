package de.unijena.bioinf.ChemistryBase.ms.ft;

import java.util.*;

public class Score extends AbstractMap<String, Double> {

    private String[] names;
    private double[] values;

    public Score(String[] names) {
        super();
        this.names = names;
        this.values = new double[names.length];
    }

    public double get(int k) {
        return values[k];
    }

    public void set(int k, double value) {
        values[k] = value;
    }

    public String getScoringMethod(int k) {
        return names[k];
    }

    @Override
    public int size() {
        return names.length;
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key)!=null;
    }

    @Override
    public Double get(Object key) {
        if (!(key instanceof String)) return null;
        for (int k=0; k < names.length; ++k)
            if (names[k].equals(key)) {
                return values[k];
            }
        return null;
    }

    @Override
    public Set<Entry<String, Double>> entrySet() {
        return new AbstractSet<Entry<String, Double>>() {
            @Override
            public Iterator<Entry<String, Double>> iterator() {
                return new Iterator<Entry<String, Double>>() {
                    int k=0;

                    @Override
                    public boolean hasNext() {
                        return k < names.length;
                    }

                    @Override
                    public Entry<String, Double> next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        final Entry<String,Double> e = new AbstractMap.SimpleEntry<String, Double>(names[k], values[k]);
                        ++k;
                        return e;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size() {
                return names.length;
            }
        };
    }

    public double sum() {
        double s=0d;
        for (int k=0; k < values.length; ++k)
            s += values[k];
        return s;
    }
}
