
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

import de.unijena.bioinf.ms.annotations.TreeAnnotation;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public final class Score implements TreeAnnotation  {

    /**
     * Mutable structure for adding scoring types
     */
    public static class HeaderBuilder {
        private LinkedHashSet<String> names;
        private String[] order;
        private String[] __nameCache;
        protected HeaderBuilder() {
            this.names = new LinkedHashSet<>();
        }

        public String define(String name) {
            names.add(name); return name;
        }

        public ScoreAssigner score() {
            determineOrder();
            return new ScoreAssigner(order);
        }

        private void determineOrder() {
            if (order==null || order.length!=names.size())
                this.order = names.toArray(new String[names.size()]);
        }
    }

    public static ScoreAdder extendWith(String newScoringName) {
        return new ScoreAdder(newScoringName);
    }

    public static class ScoreAdder {

        private String name;
        private String[] header;
        private String[] replace;
        private int pos;

        private ScoreAdder(@NotNull  String name) {
            this.name = name;
        }

        public Score add(Score s, double value) {
            if (!(header != null && Arrays.equals(header, s.names))) {
                // first check if score is already existing
                for (int k=0; k < s.names.length; ++k) {
                    if (name.equals(s.names[k])) {
                        pos = k;
                        replace = s.names;
                        header = s.names;
                    }
                }
                header = s.names;
                replace = Arrays.copyOf(header, header.length+1);
                replace[header.length] = name;
                pos = header.length;
            }
            final Score copy = new Score(replace, Arrays.copyOf(s.values,replace.length));
            copy.values[pos] = value;
            return copy;
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
                if (names[i]==null)
                    throw new NullPointerException("score name is null");
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

    private final static Score NONE = new Score(new String[0], new double[0]);

    public Score() {
        this.names = new String[0];
        this.values = new double[0];
    }

    public static Score none() {
        return NONE;
    }

    public boolean isEmpty() {
        return names.length==0;
    }

    protected Score(String[] names, double[] values) {
        super();
        this.names = names;
        this.values = values;
    }

    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("{");
        for (int i=0; i < names.length; ++i) {
            if (i > 0) buf.append(", ");
            buf.append(names[i]);
            buf.append(" = ");
            buf.append(values[i]);
        }
        buf.append("}");
        return buf.toString();
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
