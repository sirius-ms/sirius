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

import java.util.*;

public class Score extends AbstractMap<String, Double> {

    private String[] names;
    private double[] values;

    public Score(String[] names) {
        super();
        this.names = names;
        this.values = new double[names.length];
    }

    public Score extend(String... newNames) {

        final ArrayList<String> toAdd = new ArrayList<String>(Arrays.asList(newNames));
        toAdd.removeAll(Arrays.asList(names));

        final String[] allNames = Arrays.copyOf(names, names.length+toAdd.size());
        for (int k=names.length,n=names.length+toAdd.size(); k < n; ++k) {
            allNames[k] = toAdd.get(k-names.length);
        }
        final Score newScore = new Score(allNames);
        for (int k=0; k < names.length; ++k) {
            newScore.set(k, values[k]);
        }
        return newScore;
    }

    public void set(String key, double value) {
        for (int i=0; i < names.length; ++i) {
            if (names[i].equals(key)) {
                values[i] = value; return;
            }
        }
    }

    @Override
    public Double put(String key, Double value) {
        for (int i=0; i < names.length; ++i) {
            if (names[i].equals(key)) {
                double oldVal = values[i];
                values[i] = value;
                return oldVal;
            }
        }
        return null;
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
