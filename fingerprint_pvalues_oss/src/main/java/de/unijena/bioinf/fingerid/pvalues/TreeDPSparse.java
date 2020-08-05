

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

package de.unijena.bioinf.fingerid.pvalues;

import de.unijena.bioinf.graphUtils.tree.Tree;

import java.util.ArrayList;
import java.util.HashMap;

public class TreeDPSparse {

    final HashMap<Integer, DPTableSparse> tableMap;
    final ArrayList<DPTableSparse> tables;
    final FingerprintTree tree;

    protected static final class SparseMap {
        protected final Probability[] values;
        protected final int[] keys;
        protected final int offset, maxIndex;
        protected int size;
        public SparseMap(int offset, int size) {
            this.values = new Probability[size];
            this.keys = new int[size];
            this.offset = offset;
            assert offset >= 0;
            this.size = 0;
            this.maxIndex = offset+size-1;
            assert maxIndex >= offset;
        }
        public Probability get(int k) {
            final Probability p = values[k-offset];
            if (p==null) return Probability.ZERO;
            else return p;
        }
        public void set(int k, Probability p) {
            final Probability prev = values[k-offset];
            if (prev==null) {
                values[k-offset] = p;
                keys[size++] = k;
            } else {
                values[k-offset] = p;
            }
        }

        public Probability add(int k, Probability p) {
            final Probability prev = values[k-offset];
            if (prev==null) {
                values[k-offset] = p;
                keys[size++] = k;
                return p;
            } else {
                values[k-offset] = p.add(prev);
                return values[k-offset];
            }
        }

    }

    protected static final class DPTableSparse {

        private final Tree<FPVariable> node;
        private final FPVariable variable;
        private SparseMap forZero, forOne;
        private int capaOne, capaZero;
        private int offsetOne, offsetZero;

        public DPTableSparse(Tree<FPVariable> node) {
            this.node = node;
            this.variable = node.getLabel();
        }

        void reserve(int offsetZero, int offsetOne, int zeros, int ones, int maxEntries) {
            this.offsetOne = offsetOne;
            this.offsetZero = offsetZero;
            this.capaOne = (offsetOne + ones > maxEntries) ? (maxEntries-offsetOne) : ones;
            this.capaZero = (offsetZero + zeros > maxEntries) ? (maxEntries-offsetZero) : zeros;
            forZero = new SparseMap(offsetZero, this.capaZero);
            forOne = new SparseMap(offsetOne, this.capaOne);
        }

        Probability addToOne(int k, Probability value) {
            if (!value.isZeroProbability())
                return forOne.add(k, value);
            else return Probability.ZERO;
        }
        Probability addToZero(int k, Probability value) {
            if (!value.isZeroProbability())
                return forZero.add(k, value);
            else return Probability.ZERO;
        }
        Probability addToOneIndexCheck(int k, Probability value) {
            if (!value.isZeroProbability() && k <= forOne.maxIndex)
                return forOne.add(k, value);
            else return Probability.ZERO;
        }
        Probability addToZeroIndexCheck(int k, Probability value) {
            if (!value.isZeroProbability() && k <= forZero.maxIndex)
                return forZero.add(k, value);
            else return Probability.ZERO;
        }

        Probability forOne(int k) {
            return forOne.get(k);
        }
        Probability forZero(int k) {
            return forZero.get(k);
        }

        void clear() {
            this.forZero = this.forOne = null;
        }

        void copyTo(DPTableSparse other) {
            other.forZero = forZero;
            other.forOne = forOne;
            reserve(offsetZero, offsetOne, capaZero, capaOne, Integer.MAX_VALUE);
        }
    }

    public TreeDPSparse(FingerprintTree tree) {
        this.tree = tree;
        this.tableMap = new HashMap<>();
        this.tables = new ArrayList<>();
        for (Tree<FPVariable> var : tree.nodes) {
            final DPTableSparse tab = new DPTableSparse(var);
            tableMap.put(var.getLabel().to, tab);
            tables.add(tab);
        }
    }

    private static boolean checkProbability(SparseMap table) {
        Probability sum = Probability.ZERO;
        for (int i=0; i < table.size; ++i) {
            sum = sum.add(table.get(table.keys[i]));
        }
        return sum.getExp() <= 0;
    }

    public long computePlattScores(boolean[] query, int scale, double[] scoreForZero, double[] scoreForOne, double score) {
        // discretize values
        final int[] iscoreForZero = new int[scoreForZero.length];
        final int[] iscoreForOne = new int[scoreForOne.length];
        for (int k=0; k < iscoreForOne.length; ++k) {
            iscoreForOne[k] = (int)Math.round(scoreForOne[k]*scale);
            iscoreForZero[k] = (int)Math.round(scoreForZero[k]*scale);
        }
        // TODO: We might want to consider rounding errors...
        final int iscore = (int)Math.ceil(score*scale);

        for (DPTableSparse table : this.tables) {
            final int d = table.node.degree();
            if (d==0) {
                leafPlatt(table, query, iscoreForZero, iscoreForOne);
            } else if (d==1) {
                innerVertexPlatt(table, tableMap.get(table.node.children().get(0).getLabel().to), query, iscoreForZero, iscoreForOne, iscore);
            } else {
                final DPTableSparse[] tables = new DPTableSparse[d];
                int k=0;
                for (Tree<FPVariable> childNode : table.node.children()) {
                    tables[k++] = tableMap.get(childNode.getLabel().to);
                }
                multipleChildrenVertexPlatt(table, tables, query, iscoreForZero, iscoreForOne, iscore);
            }
        }
        // collect results from root node
        return rootPlatt(tableMap.get(tree.root.getLabel().to), iscore).getExp();
    }

    private void leafPlatt(DPTableSparse table, boolean[] query, int[] scoreForZero, int[] scoreForOne) {
        final FPVariable V = table.variable;
        final int i = table.variable.to;
        table.reserve(scoreForZero[i], scoreForOne[i], 1, 1, Integer.MAX_VALUE);
        table.addToOne(scoreForOne[i], V.I);
        table.addToZero(scoreForZero[i], V.o);
        assert checkProbability(table.forOne);
        assert checkProbability(table.forZero);
    }

    private void innerVertexPlatt(final DPTableSparse table, final DPTableSparse child, boolean[] query, final int[] scoreForZero, final int[] scoreForOne, final int score) {
        final FPVariable V = child.variable;
        final int i = child.variable.from;
        final int MINO = Math.min(child.offsetZero, child.offsetOne);
        final int MAXI = Math.max(child.forZero.maxIndex, child.forOne.maxIndex);
        table.reserve(
                MINO + scoreForZero[i],
                MINO + scoreForOne[i],
                MAXI - MINO + 1,
                MAXI - MINO + 1,
                score
        );

        for (int l=0; l < child.forZero.size; ++l) {
            final int d = child.forZero.keys[l];
            final Probability b = child.forZero(d);
            table.addToOneIndexCheck(d + scoreForOne[i], V.PoI.multiply(b));
            table.addToZeroIndexCheck(d + scoreForZero[i], V.Poo.multiply(b));
        }
        for (int l = 0; l < child.forOne.size; ++l) {
            final int d = child.forOne.keys[l];
            final Probability b = child.forOne(d);
            table.addToOneIndexCheck(d + scoreForOne[i], V.PII.multiply(b));
            table.addToZeroIndexCheck(d + scoreForZero[i], V.PIo.multiply(b));
        }

        assert checkProbability(table.forOne);
        assert checkProbability(table.forZero);
        child.clear();
    }

    private void multipleChildrenVertexPlatt(final DPTableSparse table, final DPTableSparse[] child, boolean[] query, int[] scoreForZero, int[] scoreForOne, final int score) {
        final DPTableSparse[] pseudoChilds = new DPTableSparse[child.length];
        for (int k=0; k < child.length; ++k) {
            pseudoChilds[k] = new DPTableSparse(table.node);
            innerVertexPlatt(pseudoChilds[k], child[k], query, scoreForZero, scoreForOne, score);
            child[k].clear();
        }
        // merge
        DPTableSparse temp2 = new DPTableSparse(table.node);
        DPTableSparse temp1 = pseudoChilds[0];
        for (int k=1; k < child.length; ++k) {
            temp2.reserve(temp1.offsetZero + pseudoChilds[k].offsetZero, temp1.offsetOne + pseudoChilds[k].offsetOne, temp1.capaZero + pseudoChilds[k].capaZero, temp1.capaOne + pseudoChilds[k].capaOne, score);
            for (int l=0; l < temp1.forOne.size; ++l) {
                final int a = temp1.forOne.keys[l];
                final Probability b = temp1.forOne(a);
                if (!b.isZeroProbability()) for (int m=0; m < pseudoChilds[k].forOne.size; ++m) {
                    final int xa = pseudoChilds[k].forOne.keys[m];
                    final Probability xb = pseudoChilds[k].forOne(xa);
                    temp2.addToOneIndexCheck(a + xa, b.multiply(xb));
                }
            }
            for (int l=0; l < temp1.forZero.size; ++l) {
                final int a = temp1.forZero.keys[l];
                final Probability b = temp1.forZero(a);
                if (!b.isZeroProbability()) for (int m=0; m < pseudoChilds[k].forZero.size; ++m) {
                    final int xa = pseudoChilds[k].forZero.keys[m];
                    final Probability xb = pseudoChilds[k].forZero(xa);
                    temp2.addToZeroIndexCheck(a + xa, b.multiply(xb));
                }
            }
            temp1 = temp2;
            temp2 = new DPTableSparse(table.node);
        }
        table.forOne = temp1.forOne;
        table.forZero = temp1.forZero;
        for (DPTableSparse pchild : pseudoChilds) {
            pchild.clear();
        }
        assert checkProbability(table.forOne);
        assert checkProbability(table.forZero);
    }

    private Probability rootPlatt(DPTableSparse table, int score) {
        Probability pvalue = Probability.ZERO;
        for (int k=table.offsetOne; k <= Math.min(table.forOne.maxIndex, score); ++k) {
            pvalue = pvalue.add(table.forOne(k));
        }
        for (int k=table.offsetZero; k <= Math.min(table.forZero.maxIndex, score); ++k) {
            pvalue = pvalue.add(table.forZero(k));
        }
        return pvalue;
    }

}
