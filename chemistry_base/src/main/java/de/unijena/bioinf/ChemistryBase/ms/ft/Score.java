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

import de.unijena.bioinf.ms.annotations.TreeAnnotation;

import java.util.*;


public final class Score implements TreeAnnotation  {

    /**
     * Mutable structure for adding scoring types
     */
    public static class HeaderBuilder {
        private LinkedHashSet<String> names;
        private String[] order;
        protected HeaderBuilder() {
            this.names = new LinkedHashSet<>();
        }

        public String define(String name) {
            names.add(name); return name;
        }

        public ScoreAssigner score() {
            if (order==null || order.length!=names.size())
                this.order = names.toArray(new String[names.size()]);
            return new ScoreAssigner(order);
        }
    }

    /**
     * Mutable structure for assigning scores. At this point, the scoring types
     * are fixed and the underlying array data structure can be shared between fragmentation trees to
     * save memory
     */
    public static class ScoreAssigner {
        private HashMap<String, Double> map;
        private String[] names;

        protected ScoreAssigner(String[] names) {
            this.map = new HashMap<>(names.length);
            this.names = names;
            for (String key : names) map.put(key,0d);
        }

        protected ScoreAssigner(Score map) {
            this.map = new HashMap<>(map.asMap());
            this.names = map.names;
        }

        public void set(String key, double value) {
            if (!map.containsKey(key))
                throw new NoSuchElementException("Unknown score: " + key);
            map.put(key, value);
        }

        public double get(String key) {
            if (!map.containsKey(key))
                throw new NoSuchElementException("Unknown score: " + key);
            return map.get(key);
        }

        public Score done() {
            final double[] scores = new double[map.size()];
            for (int i=0; i < names.length; ++i) {
                scores[i] = map.get(names[i]);
            }
            return new Score(names, scores);
        }
    }

    public static HeaderBuilder defineScoring() {
        return new HeaderBuilder();
    }

    private final String[] names;
    private final double[] values;

    protected Score(String[] names, double[] values) {
        super();
        this.names = names;
        this.values = values;
    }

    public double get(int k) {
        return values[k];
    }

    public double get(String name) {
        for (int i=0; i < names.length; ++i) {
            if (names[i].equals(name)) {
                return values[i];
            }
        }
        return 0d;
    }

    public String getScoringMethod(int k) {
        return names[k];
    }

    public int size() {
        return names.length;
    }


    public Map<String, Double> asMap() {
        return new AbstractMap<String, Double>() {

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
        };
    }

    public double sum() {
        double s=0d;
        for (int k=0; k < values.length; ++k)
            s += values[k];
        return s;
    }
}
