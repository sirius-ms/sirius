

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
import java.util.Arrays;
import java.util.HashMap;

public class TreeDP {

    final HashMap<Integer, DPTableUnit> tableMap;
    final ArrayList<DPTableUnit> tables;
    final FingerprintTree tree;

    protected static final class DPTableUnit {

        private final Tree<FPVariable> node;
        private final FPVariable variable;
        private Probability[] forZero, forOne;
        private int capaOne, capaZero;

        public DPTableUnit(Tree<FPVariable> node) {
            this.node = node;
            this.variable = node.getLabel();
        }

        void reserve(int capa) {
            reserve(capa,capa);
        }

        void reserve(int zeros, int ones) {
            this.capaOne = ones;
            this.capaZero = zeros;
            forZero = new Probability[capaZero];
            Arrays.fill(forZero, Probability.ZERO);
            forOne = new Probability[capaOne];
            Arrays.fill(forOne, Probability.ZERO);
        }

        void clear() {
            this.forZero = this.forOne = null;
        }

        void copyTo(DPTableUnit other) {
            System.arraycopy(forOne, 0, other.forOne, 0, forOne.length);
            System.arraycopy(forZero, 0, other.forZero, 0, forZero.length);
        }
    }

    public TreeDP(FingerprintTree tree) {
        this.tree = tree;
        this.tableMap = new HashMap<>();
        this.tables = new ArrayList<>();
        for (Tree<FPVariable> var : tree.nodes) {
            final DPTableUnit tab = new DPTableUnit(var);
            tableMap.put(var.getLabel().to, tab);
            tables.add(tab);
        }
    }

    private static boolean checkProbability(Probability[] table) {
        Probability sum = Probability.ZERO;
        for (int k=0; k < table.length; ++k) {
            sum=sum.add(table[k]);
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

        for (DPTableUnit table : this.tables) {
            final int d = table.node.degree();
            if (d==0) {
                leafPlatt(table, query, iscoreForZero, iscoreForOne);
            } else if (d==1) {
                innerVertexPlatt(table, tableMap.get(table.node.children().get(0).getLabel().to), query, iscoreForZero, iscoreForOne, iscore);
            } else {
                final DPTableUnit[] tables = new DPTableUnit[d];
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

    public long computeUnitScores(boolean[] query, int score) {
        for (DPTableUnit table : this.tables) {
            final int d = table.node.degree();
            if (d==0) {
                leafUnit(table, query);
            } else if (d==1) {
                innerVertexUnit(table, tableMap.get(table.node.children().get(0).getLabel().to), query, score);
            } else {
                final DPTableUnit[] tables = new DPTableUnit[d];
                int k=0;
                for (Tree<FPVariable> childNode : table.node.children()) {
                    tables[k++] = tableMap.get(childNode.getLabel().to);
                }
                multipleChildrenVertexUnit(table, tables, query, score);
            }
        }
        // collect results from root node
        return rootUnit(tableMap.get(tree.root.getLabel().to), score).getExp();
    }

    private Probability rootUnit(DPTableUnit table, int score) {
        Probability pvalue = Probability.ZERO;
        for (int k=0; k <= score; ++k) {
            pvalue = pvalue.add(table.forOne[k]).add(table.forZero[k]);
        }
        return pvalue;
    }

    private void multipleChildrenVertexUnit(DPTableUnit table, DPTableUnit[] child, boolean[] query, int score) {
        final DPTableUnit[] pseudoChilds = new DPTableUnit[child.length];
        int capas = 0;
        for (int k=0; k < child.length; ++k) {
            pseudoChilds[k] = new DPTableUnit(table.node);
            innerVertexUnit(pseudoChilds[k], child[k], query, score);
            child[k].clear();
            capas += pseudoChilds[k].capaOne;
            assert pseudoChilds[k].capaOne==pseudoChilds[k].capaZero;
        }
        capas = Math.min(capas, score+1);
        // merge
        final DPTableUnit temp = new DPTableUnit(table.node);
        temp.reserve(capas);
        table.reserve(capas);
        pseudoChilds[0].copyTo(temp);
        for (int k=1; k < child.length; ++k) {
            final int N = Math.min(score+1, capas );
            for (int d=0; d < N; ++d) {
                final int M = Math.min(d, pseudoChilds[k].capaOne-1);
                for (int e=0; e <= M; ++e) {
                    table.forOne[d] = table.forOne[d].add(temp.forOne[d-e].multiply(pseudoChilds[k].forOne[e]));
                    table.forZero[d] = table.forZero[d].add(temp.forZero[d-e].multiply(pseudoChilds[k].forZero[e]));
                }
            }
            table.copyTo(temp);
            Arrays.fill(table.forZero, Probability.ZERO);
            Arrays.fill(table.forOne, Probability.ZERO);
        }
        temp.copyTo(table);
        for (DPTableUnit pchild : pseudoChilds) {
            pchild.clear();
        }
        assert checkProbability(table.forOne);
        assert checkProbability(table.forZero);
    }

    private void innerVertexUnit(DPTableUnit table, DPTableUnit child, boolean[] query, int score) {
        final FPVariable V = child.variable;
        final boolean u = query[V.from], v = query[V.to];
        final int N = Math.min(score, child.capaOne);
        assert child.capaOne==child.capaZero;
        table.reserve(N + 1);
        for (int d=0; d < N; ++d) {
            table.forOne[d+(u?0:1)] = V.PII.multiply(child.forOne[d]).add(  V.PoI.multiply(child.forZero[d]) );
            table.forZero[d+(u?1:0)] = V.PIo.multiply(child.forOne[d]).add(  V.Poo.multiply(child.forZero[d]) );
        }
        assert checkProbability(table.forOne);
        assert checkProbability(table.forZero);
        child.clear();
    }

    private void leafUnit(DPTableUnit table, boolean[] query) {
        final FPVariable V = table.variable;
        final boolean v = query[table.variable.to];
        table.reserve(2);
        if (v) {
            table.forOne[0] = V.I;
            table.forZero[1] = V.o;
        } else {
            table.forOne[1] = V.I;
            table.forZero[0] = V.o;
        }
        assert checkProbability(table.forOne);
        assert checkProbability(table.forZero);
    }

    private void leafPlatt(DPTableUnit table, boolean[] query, int[] scoreForZero, int[] scoreForOne) {
        final FPVariable V = table.variable;
        final int i = table.variable.to;
        table.reserve(scoreForZero[i]+1, scoreForOne[i]+1);
        table.forOne[scoreForOne[i]] = V.I;
        table.forZero[scoreForZero[i]] = V.o;
        assert checkProbability(table.forOne);
        assert checkProbability(table.forZero);
    }

    private void innerVertexPlatt(DPTableUnit table, DPTableUnit child, boolean[] query, int[] scoreForZero, int[] scoreForOne, int score) {
        final FPVariable V = child.variable;
        final int i = child.variable.from;
        final int Z = Math.min(score, Math.max(child.capaOne-1, child.capaZero-1) + scoreForZero[i]) + 1;
        final int O = Math.min(score, Math.max(child.capaOne-1, child.capaZero-1) + scoreForOne[i]) + 1;
        table.reserve(Z, O);

        final int childZero = Math.min(score+1, child.capaZero);
        for (int d=0; d < childZero; ++d) {
            if (d + scoreForOne[i] <= score)
                table.forOne[d + scoreForOne[i]] = table.forOne[d + scoreForOne[i]].add(V.PoI.multiply(child.forZero[d]));
            if (d + scoreForZero[i] <= score)
                table.forZero[d + scoreForZero[i]] = table.forZero[d + scoreForZero[i]].add(V.Poo.multiply(child.forZero[d]));
        }
        final int childOne = Math.min(score+1, child.capaOne);
        for (int d=0; d < childOne; ++d) {
            if (d + scoreForOne[i] <= score)
                table.forOne[d + scoreForOne[i]] = table.forOne[d + scoreForOne[i]].add(V.PII.multiply(child.forOne[d]));

            if (d + scoreForZero[i] <= score)
                table.forZero[d + scoreForZero[i]] = table.forZero[d + scoreForZero[i]].add(V.PIo.multiply(child.forOne[d]));
        }
        assert checkProbability(table.forOne);
        assert checkProbability(table.forZero);
        child.clear();
    }

    private void multipleChildrenVertexPlatt(DPTableUnit table, DPTableUnit[] child, boolean[] query, int[] scoreForZero, int[] scoreForOne, int score) {
        final DPTableUnit[] pseudoChilds = new DPTableUnit[child.length];
        int capaOnes = 0, capaZeros=0;
        for (int k=0; k < child.length; ++k) {
            pseudoChilds[k] = new DPTableUnit(table.node);
            innerVertexPlatt(pseudoChilds[k], child[k], query, scoreForZero, scoreForOne, score);
            child[k].clear();
            capaOnes += pseudoChilds[k].capaOne;
            capaZeros += pseudoChilds[k].capaZero;
        }
        // merge
        final DPTableUnit temp = new DPTableUnit(table.node);
        temp.reserve(Math.min(score+1, capaZeros), Math.min(score+1, capaOnes));
        table.reserve(Math.min(score+1, capaZeros), Math.min(score+1, capaOnes));
        pseudoChilds[0].copyTo(temp);
        for (int k=1; k < child.length; ++k) {
            for (int d=0; d < temp.capaOne; ++d) {
                if (temp.forOne[d].isZeroProbability()) continue;
                for (int e=0; e < pseudoChilds[k].capaOne; ++e) {
                    if (d+e > score) break;
                    table.forOne[d+e] = table.forOne[d+e].add(temp.forOne[d].multiply(pseudoChilds[k].forOne[e]));
                }
            }
            for (int d=0; d < temp.capaZero; ++d) {
                if (temp.forZero[d].isZeroProbability()) continue;
                for (int e=0; e < pseudoChilds[k].capaZero; ++e) {
                    if (d+e > score) break;
                    table.forZero[d+e] = table.forZero[d+e].add(temp.forZero[d].multiply(pseudoChilds[k].forZero[e]));
                }
            }
            table.copyTo(temp);
            Arrays.fill(table.forZero, Probability.ZERO);
            Arrays.fill(table.forOne, Probability.ZERO);
        }
        temp.copyTo(table);
        for (DPTableUnit pchild : pseudoChilds) {
            pchild.clear();
        }
        assert checkProbability(table.forOne);
        assert checkProbability(table.forZero);
    }

    private Probability rootPlatt(DPTableUnit table, int score) {
        Probability pvalue = Probability.ZERO;
        for (int k=0; k <= score; ++k) {
            pvalue = pvalue.add(table.forOne[k]).add(table.forZero[k]);
        }
        return pvalue;
    }

}
