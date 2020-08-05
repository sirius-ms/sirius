/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *  
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker, 
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

package de.unijena.bioinf.ftalign.view;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.ftalign.StandardScoring;
import de.unijena.bioinf.treealign.AbstractBacktrace;
import de.unijena.bioinf.treealign.sparse.DPSparseTreeAlign;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;
import org.json.JSONException;
import org.json.JSONWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

public class MakeStatistics {

    public static void main(String[] args) {
        new MakeStatistics().run();

    }

    private final StandardScoring scoring;
    private final HashMap<MolecularFormula, MolecularFormula> cache;
    private final Statistics statistics;

    public MakeStatistics() {
        this.scoring = new StandardScoring(true);
        scoring.gapScore = -10;
        scoring.matchScore = 15;
        scoring.scoreForEachNonHydrogen = 5;
        scoring.lossMatchScore = 10;
        scoring.penaltyForEachNonHydrogen = -0.5f;
        scoring.penaltyForEachJoin = -2;
        scoring.joinMissmatchPenalty = -10;
        this.cache = new HashMap<>();
        this.statistics = new Statistics();
    }

    public MolecularFormula cache(MolecularFormula f) {
        final MolecularFormula cached = cache.get(f);
        if (cached != null) return cached;
        else {
            cache.put(f,f);
            return f;
        }
    }

    public void run() {
        System.out.println("READ INPUT");
        final List<DataElement> elems = readInput();
        // find pairs with Tanimoto >= 0.9
        System.out.println("FIND SIMILAR COMPOUNDS");
        final List<Pair> pairs = new ArrayList<>();
        for (int i=0; i < elems.size(); ++i) {
            for (int j=i+1; j < elems.size(); ++j) {
                final Pair pair = new Pair(elems.get(i), elems.get(j));
                if (pair.getTanimoto() >= 0.9) {
                    pairs.add(pair);
                }
            }
        }
        System.out.println("FOUND " + pairs.size() + " PAIRS");
        System.out.println("COMPUTE ALIGNMENTS");


        for (Pair pair : pairs) {
            try {
                align(pair);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }

        statistics.writeToFile();

    }

    private List<DataElement> readInput() {
        final List<DataElement> elems = new ArrayList<>();
        for (File f : new File("trees").listFiles()) {
            if (f.getName().endsWith(".json")) {
                try {
                    final String name = f.getName().substring(0, f.getName().indexOf(".json"));
                    final DataElement elem = new DataElement(name);
                    if (new File("fingerprints", name + ".fpt").exists()) {
                        final FTree tree = new GenericParser<FTree>(new FTJsonReader()).parseFromFile(f).get(0);
                        elem.setTree(tree);
                        final BitSet fingerprint = new BitSet(6269);
                        final String line = Files.readAllLines(new File("fingerprints", name + ".fpt").toPath(), Charset.defaultCharset()).get(0);
                        for (int i=0; i < line.length(); ++i) {
                            if (line.charAt(i)=='1') fingerprint.set(i);
                        }
                        elem.setFingerprint(fingerprint);
                        elems.add(elem);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return elems;
    }

    private void align(Pair pair) {
        final DPSparseTreeAlign<Fragment> aligner = new DPSparseTreeAlign<Fragment>(new StandardScoring(true), true, pair.getLeft().getTree().getRoot(),
                pair.getRight().getTree().getRoot(),
                FTree.treeAdapterStatic());
        final double value = aligner.compute();
        aligner.backtrace(statistics);

    }

    private static class Counter {
        private final TObjectIntHashMap<MolecularFormula> counter;

        public Counter() {
            this.counter = new TObjectIntHashMap<>();
        }

        public int count(MolecularFormula f) {
            return counter.adjustOrPutValue(f, 1, 1);
        }

        public int get(MolecularFormula f) {
            return counter.get(f);
        }

    }

    private static class Counter2 {
        private final HashMap<MolecularFormula, ArrayList<MolecularFormula>> map;

        public Counter2() {
            this.map = new HashMap<>();
        }

        public void add(MolecularFormula f, MolecularFormula g) {
            addAsy(f, g);
            if (!f.equals(g)) {
                addAsy(g, f);
            }
        }

        private void addAsy(MolecularFormula f, MolecularFormula g) {
            if (!map.containsKey(f)) {
                map.put(f, new ArrayList<MolecularFormula>());
            }
            map.get(f).add(g);
        }

    }

    private class Statistics extends AbstractBacktrace<Fragment> {

        private Counter deletedLosses = new Counter();
        private Counter2 matchedLosses = new Counter2();
        private Counter2 matchedFragments = new Counter2();

        @Override
        public void deleteLeft(float score, Fragment node) {
            super.deleteLeft(score, node);
            deletedLosses.count(cache(node.getFormula()));
        }

        @Override
        public void deleteRight(float score, Fragment node) {
            super.deleteRight(score, node);
            deletedLosses.count(cache(node.getFormula()));
        }

        @Override
        public void match(float score, Fragment left, Fragment right) {
            super.match(score, left, right);
            matchedFragments.add(cache(left.getFormula()), cache(right.getFormula()));
            matchedLosses.add(cache(left.getIncomingEdge().getFormula()), cache(right.getIncomingEdge().getFormula()));
        }

        @Override
        public void innerJoinLeft(Fragment node) {
            super.innerJoinLeft(node);
        }

        @Override
        public void innerJoinRight(Fragment node) {
            super.innerJoinRight(node);
        }

        @Override
        public void matchVertices(float score, Fragment left, Fragment right) {
            super.matchVertices(score, left, right);
            matchedFragments.add(cache(left.getFormula()), cache(right.getFormula()));
        }

        @Override
        public void join(float score, Iterator<Fragment> left, Iterator<Fragment> right, int leftNumber, int rightNumber) {
            super.join(score, left, right, leftNumber, rightNumber);
        }

        public void writeToFile() {
            try {

                final BufferedWriter bw = Files.newBufferedWriter(new File("statistics.json").toPath(), Charset.forName("UTF-8"));
                final JSONWriter w = new JSONWriter(bw);

                w.object();

                w.key("deletedLosses");

                w.object();
                deletedLosses.counter.forEachEntry(new TObjectIntProcedure<MolecularFormula>() {
                    @Override
                    public boolean execute(MolecularFormula a, int b) {
                        try {
                            w.key(a.toString());
                            w.value(b);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                });
                w.endObject();

                w.key("matchedFragments");
                w.object();
                for (Map.Entry<MolecularFormula, ArrayList<MolecularFormula>> entry : matchedFragments.map.entrySet()) {
                    w.key(entry.getKey().toString());
                    w.object();
                    final Counter c = new Counter();
                    for (MolecularFormula f : entry.getValue()) {
                        c.count(f);
                    }
                    c.counter.forEachEntry(new TObjectIntProcedure<MolecularFormula>() {
                        @Override
                        public boolean execute(MolecularFormula a, int b) {
                            try {
                                w.key(a.toString());
                                w.value(b);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return true;
                        }
                    });
                    w.endObject();
                }
                w.endObject();

                w.key("matchedLosses");
                w.object();
                for (Map.Entry<MolecularFormula, ArrayList<MolecularFormula>> entry : matchedLosses.map.entrySet()) {
                    w.key(entry.getKey().toString());
                    w.object();
                    final Counter c = new Counter();
                    for (MolecularFormula f : entry.getValue()) {
                        c.count(f);
                    }
                    c.counter.forEachEntry(new TObjectIntProcedure<MolecularFormula>() {
                        @Override
                        public boolean execute(MolecularFormula a, int b) {
                            try {
                                w.key(a.toString());
                                w.value(b);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return true;
                        }
                    });
                    w.endObject();
                }
                w.endObject();

                w.endObject();

                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

}
