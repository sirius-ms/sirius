
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

package de.unijena.bioinf.treealign.dp;

import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;
import de.unijena.bioinf.graphUtils.tree.TreeAdapter;
import de.unijena.bioinf.graphUtils.tree.TreeCursor;
import de.unijena.bioinf.treealign.*;
import de.unijena.bioinf.treealign.scoring.Scoring;
import de.unijena.bioinf.util.Iterators;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static de.unijena.bioinf.treealign.Set.subList;

public class DPTreeAlign<T> implements TreeAlignmentAlgorithm<T> {

    // algorithm
    private final Scoring<T> scoring;
    private final Tree<T> left;
    private final Tree<T> right;
    private final TreeMap<T> tables;
    private final ArrayList<Tree<T>> leftVertices;
    private final ArrayList<Tree<T>> rightVertices;
    private final ArrayList<Tree<T>> leftLeafs;
    private final ArrayList<Tree<T>> rightLeafs;

    private final int maxDegree;
    private final TreeAdapter<T> adapter;
    private final ArrayDeque<TraceItem<T>> queue;
    private final int[][] supersets;
    private final boolean useJoins;
    // result
    private Tree<T> optLeft;
    private Tree<T> optRight;
    private float optScore;
    // backtracking
    private Backtrace<T> tracer;
    private boolean scoreRoot;

    public DPTreeAlign(Scoring<T> scoring, boolean useJoins, T left, T right, TreeAdapter<T> adapter) {
        this.adapter = adapter;
        this.scoring = scoring;
        final int leftSize = TreeCursor.getCursor(left, adapter).numberOfVertices();
        final int rightSize = TreeCursor.getCursor(right, adapter).numberOfVertices();
        this.leftVertices = new ArrayList<Tree<T>>(leftSize);
        this.rightVertices = new ArrayList<Tree<T>>(rightSize);
        this.leftLeafs = new ArrayList<Tree<T>>(leftSize);
        this.rightLeafs = new ArrayList<Tree<T>>(rightSize);
        final TreeDecorator<T> leftDeco = new TreeDecorator<T>(leftVertices, leftLeafs);
        this.left = new PostOrderTraversal<T>(left, adapter).call(leftDeco);
        final TreeDecorator<T> rightDeco = new TreeDecorator<T>(rightVertices, rightLeafs);
        this.right = new PostOrderTraversal<T>(right, adapter).call(rightDeco);
        this.tables = new TreeMap<T>(leftVertices.size(), rightVertices.size());
        this.queue = new ArrayDeque<TraceItem<T>>();
        this.maxDegree = Math.max(leftDeco.maxDegree, rightDeco.maxDegree);
        this.optScore = -1;
        this.supersets = new int[1 << maxDegree][];
        Set.generateSubsetsUntil(supersets, (1 << maxDegree) - 1);
        this.scoreRoot = false;
        this.useJoins = useJoins;
    }

    protected float scoreSubtreeRoots() {
        if (scoreRoot) return optScore;
        assert left.isRoot();
        assert right.isRoot();
        final Iterator<Tree<T>> iter = PostOrderTraversal.create(left).iterator();
        float opt = 0f;
        while (iter.hasNext()) {
            final Tree<T> a = iter.next();
            final Iterator<Tree<T>> iter2 = PostOrderTraversal.create(right).iterator();
            while (iter2.hasNext()) {
                final Tree<T> b = iter2.next();
                final Table<T> table = tables.get(a.index, b.index);
                final float newScore = table.getScore() + scoring.scoreVertices(a.label, b.label);
                if (newScore > opt) {
                    this.optLeft = a;
                    this.optRight = b;
                    opt = newScore;
                }
            }
        }
        this.optScore = opt;
        this.scoreRoot = true;
        return optScore;
    }

    public float compute() {
        this.optScore = 0.0f;
        this.optLeft = left;
        this.optRight = right;
        for (int i = 0; i < leftVertices.size(); ++i) {
            final Tree<T> u = leftVertices.get(i);
            final int fullSetLeft = 1 << u.degree();
            for (int j = 0; j < rightVertices.size(); ++j) {
                final Tree<T> v = rightVertices.get(j);
                final int fullSetRight = 1 << v.degree();
                tables.set(i, j, new Table<T>(u.children(), v.children(), useJoins));
                final Table<T> D = tables.get(i, j);

                for (int x = 0; x < u.degree(); ++x) {
                    final Tree<T> a = u.children().get(x);
                    for (int y = 0; y < v.degree(); ++y) {
                        final Tree<T> b = v.children().get(y);
                        final float subtreeScore = tables.get(a.index, b.index).getScore();
                        D.set(a.key, b.key, scoring.match(a.label, b.label) + subtreeScore);
                        if (useJoins && !u.isRoot()) {
                            D.setPrejoinLeft(a.key, b.key, scoring.joinLeft(u.label, a.label, b.label) + subtreeScore);
                        }
                        if (useJoins && !v.isRoot()) {
                            D.setPrejoinRight(a.key, b.key, scoring.joinRight(v.label, b.label, a.label) + subtreeScore);
                        }
                    }
                }

                for (int A = 1; A < fullSetLeft; ++A) {
                    final List<Tree<T>> As = subList(u.children(), A);
                    for (int B = 1; B < fullSetRight; ++B) {
                        final List<Tree<T>> Bs = subList(v.children(), B);

                        if (useJoins) {
                            // prejoin
                            if (!u.isRoot()) {
                                D.setPrejoinLeft(A, B, preJoinLeft(u, v, D, A, B, As, Bs));
                            }
                            if (!v.isRoot()) {
                                D.setPrejoinRight(A, B, preJoinRight(u, v, D, A, B, As, Bs));
                            }
                        }
                        float score = 0.0f;
                        score = Math.max(score, match(u, v, D, A, B, As, Bs));
                        score = Math.max(score, deleteLeft(u, v, D, A, B, As, Bs));
                        score = Math.max(score, deleteRight(u, v, D, A, B, As, Bs));
                        if (useJoins) {
                            score = Math.max(score, joinLeft(u, v, D, A, B, As, Bs));
                            score = Math.max(score, joinRight(u, v, D, A, B, As, Bs));
                        }
                        D.set(A, B, score);
                    }
                }
                final float vertexScore = vertexScore(D, u, v);
                if (vertexScore > optScore) {
                    this.optLeft = u;
                    this.optRight = v;
                    this.optScore = vertexScore;
                }
            }
        }
        if (scoring.isScoringVertices()) {
            return scoreSubtreeRoots();
        } else return optScore;
    }

    private float vertexScore(Table<T> table, Tree<T> u, Tree<T> v) {
        final float matchScore = table.getScore();
        return matchScore;
    }

    @Override
    public void backtrace(Backtrace<T> tracer) {
        if (optScore <= 0f) return;
        this.tracer = tracer;
        this.queue.clear();
        if (scoreRoot) {
            tracer.matchVertices(optScore - tables.get(optLeft.index, optRight.index).getScore(), optLeft.label, optRight.label);
        }
        queue.offer(new TraceItem<T>(optLeft, optRight));
        while (!queue.isEmpty()) {
            final TraceItem<T> item = queue.poll();
            final List<Tree<T>> As = subList(item.u.children(), item.A);
            final List<Tree<T>> Bs = subList(item.v.children(), item.B);
            final Table<T> D = tables.get(item.u.index, item.v.index);
            final float opt = D.get(item.A, item.B);
            if (opt == 0) continue;
            boolean found = traceMatch(item.u, item.v, opt, D, item.A, item.B, As, Bs) ||
                    traceDeleteLeft(item.u, item.v, opt, D, item.A, item.B, As, Bs) ||
                    traceDeleteRight(item.u, item.v, opt, D, item.A, item.B, As, Bs) ||
                    (useJoins && tracejoinLeft(item.u, item.v, opt, D, item.A, item.B, As, Bs)) ||
                    (useJoins && tracejoinRight(item.u, item.v, opt, D, item.A, item.B, As, Bs));
            if (!found) {
                throw new RuntimeException("No Backtrace is found!");
            }
        }
    }

    private boolean traceMatch(Tree<T> u, Tree<T> v, float opt, final Table<T> D,
                               final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        for (Tree<T> a : As) {
            final int A_ = A & ~a.key;
            for (Tree<T> b : Bs) {
                final int B_ = B & ~b.key;
                float score = tables.get(a.index, b.index).getScore() + D.get(A_, B_) + scoring.match(a.label, b.label);
                if (score >= opt) {
                    assert score == opt;
                    assert score == match(u, v, D, A, B, As, Bs);
                    tracer.match(scoring.match(a.label, b.label), a.label, b.label);
                    if (a.degree() > 0 && b.degree() > 0) {
                        queue.offer(new TraceItem<T>(a, b));
                    } else {
                        assert tables.get(a.index, b.index).getScore() == 0f;
                    }
                    queue.offer(new TraceItem<T>(u, v, A_, B_));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean traceDeleteLeft(Tree<T> u, Tree<T> v, float opt, final Table<T> D,
                                    final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        final int[] B_subsets = getSubsets(B);
        for (int i = 0; i < B_subsets.length; ++i) {
            final int B_ = B_subsets[i];
            final int BB = B & ~B_;
            for (Tree<T> a : As) {
                final int A_ = A & ~a.key;
                if (a.degree() == 0) continue;
                final float score = tables.get(a.index, v.index).get(Set.of(a.children()), B_) +
                        D.get(A_, BB) + scoring.deleteLeft(a.label);
                if (score >= opt) {
                    assert score == opt;
                    assert score == deleteLeft(u, v, D, A, B, As, Bs);
                    tracer.deleteLeft(scoring.deleteLeft(a.label), a.label);
                    if (a.degree() > 0)
                        queue.offer(new TraceItem<T>(a, v, Set.of(a.children()), B_));
                    queue.offer(new TraceItem<T>(u, v, A_, BB));
                    return true;
                }

            }
        }
        return false;
    }

    private boolean traceDeleteRight(Tree<T> u, Tree<T> v, float opt, final Table<T> D,
                                     int A, int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        final int[] A_subsets = getSubsets(A);
        for (int i = 0; i < A_subsets.length; ++i) {
            final int A_ = A_subsets[i];
            final int AA = A & ~A_;
            for (Tree<T> b : Bs) {
                final int B_ = B & ~b.key;
                if (b.degree() == 0) continue;
                final float score = tables.get(u.index, b.index).get(A_, Set.of(b.children())) +
                        D.get(AA, B_) + scoring.deleteRight(b.label);
                if (score >= opt) {
                    assert score == opt;
                    assert score == deleteRight(u, v, D, A, B, As, Bs);
                    tracer.deleteRight(scoring.deleteRight(b.label), b.label);
                    if (b.degree() > 0)
                        queue.offer(new TraceItem<T>(u, b, A_, Set.of(b.children())));
                    queue.offer(new TraceItem<T>(u, v, AA, B_));
                    return true;
                }

            }
        }
        return false;
    }

    private boolean tracejoinLeft(Tree<T> u, Tree<T> v, float opt, final Table<T> D,
                                  int A, int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        final int[] B_subsets = getSubsets(B);
        eachChild:
        for (Tree<T> a : As) {
            if (a.degree() == 0) continue;
            final Table<T> T = tables.get(a.index, v.index);
            final int A_ = A & ~(a.key);
            for (int k = 1; k < B_subsets.length; ++k) {
                final int B_ = B_subsets[k];
                final int BB = B & ~B_;
                final float score = T.getPrejoinLeft(Set.of(a.children()), B_) +
                        D.get(A_, BB);
                if (score >= opt) {
                    assert score == opt;
                    assert score == joinLeft(u, v, D, A, B, As, Bs);
                    // trace prejoin
                    int leftSet = Set.of(a.children());
                    List<Tree<T>> leftList = new ArrayList<Tree<T>>(a.children());
                    int rightSet = B_;
                    List<Tree<T>> rightList = subList(v.children(), B_);
                    setLoop:
                    while (leftSet > 0 && rightSet > 0) {
                        final float searchedScore = T.getPrejoinLeft(leftSet, rightSet);
                        if (searchedScore <= 0) break;
                        for (int i = 0; i < leftList.size(); ++i) {
                            final Tree<T> x = leftList.get(i);
                            final int newLeftSet = leftSet & ~(x.key);
                            for (int j = 0; j < rightList.size(); ++j) {
                                final Tree<T> y = rightList.get(j);
                                final int newRightSet = rightSet & ~(y.key);
                                final float s = T.getPrejoinLeft(newLeftSet, newRightSet) + scoring.joinLeft(a.label, x.label, y.label)
                                        + tables.get(x.index, y.index).getScore();
                                if (s >= searchedScore) {
                                    assert s == searchedScore;
                                    tracer.innerJoinLeft(a.label);
                                    tracer.join(scoring.joinLeft(a.label, x.label, y.label), x.eachAncestors(1),
                                            Iterators.singleton(y.label), 2, 1);
                                    if (x.degree() > 0 && y.degree() > 0) {
                                        queue.offer(new TraceItem<T>(x, y));
                                    } else {
                                        assert tables.get(x.index, y.index).getScore() == 0f;
                                    }
                                    leftSet = leftSet & ~(x.key);
                                    rightSet = rightSet & ~(y.key);
                                    leftList.remove(i);
                                    rightList.remove(j);
                                    continue setLoop;
                                }
                            }
                        }
                        assert false;
                        continue eachChild;
                    }
                    // trace other siblings
                    queue.offer(new TraceItem<T>(u, v, A_, BB));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tracejoinRight(Tree<T> u, Tree<T> v, float opt, final Table<T> D,
                                   final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        final int[] A_subsets = getSubsets(A);
        eachChild:
        for (Tree<T> b : Bs) {
            if (b.degree() == 0) continue;
            final Table<T> T = tables.get(u.index, b.index);
            final int B_ = B & ~(b.key);
            for (int k = 1; k < A_subsets.length; ++k) {
                final int A_ = A_subsets[k];
                final int AA = A & ~(A_);
                final float score = T.getPrejoinRight(A_, Set.of(b.children())) +
                        D.get(AA, B_);
                if (score >= opt) {
                    assert score == opt;
                    assert score == joinRight(u, v, D, A, B, As, Bs);
                    // trace prejoin
                    int rightSet = Set.of(b.children());
                    List<Tree<T>> rightList = new ArrayList<Tree<T>>(b.children());
                    int leftSet = A_;
                    List<Tree<T>> leftList = subList(u.children(), A_);
                    setLoop:
                    while (leftSet > 0 && rightSet > 0) {
                        final float searchedScore = T.getPrejoinRight(leftSet, rightSet);
                        if (searchedScore <= 0) break;
                        for (int i = 0; i < leftList.size(); ++i) {
                            final Tree<T> x = leftList.get(i);
                            final int newLeftSet = leftSet & ~(x.key);
                            for (int j = 0; j < rightList.size(); ++j) {
                                final Tree<T> y = rightList.get(j);
                                final int newRightSet = rightSet & ~(y.key);
                                tracer.innerJoinRight(b.label);
                                final float joinScore = scoring.joinRight(b.label, y.label, x.label);
                                final float s = T.getPrejoinRight(newLeftSet, newRightSet) + joinScore
                                        + tables.get(x.index, y.index).getScore();
                                if (s >= searchedScore) {
                                    assert s == searchedScore;
                                    tracer.join(joinScore, Iterators.singleton(x.label), y.eachAncestors(1), 1, 2);
                                    if (x.degree() > 0 && y.degree() > 0)
                                        queue.offer(new TraceItem<T>(x, y));
                                    leftSet = leftSet & ~(x.key);
                                    rightSet = rightSet & ~(y.key);
                                    leftList.remove(i);
                                    rightList.remove(j);
                                    continue setLoop;
                                }
                            }
                        }
                        assert false;
                        continue eachChild;
                    }
                    // trace other siblings
                    queue.offer(new TraceItem<T>(u, v, AA, B_));
                    return true;
                }
            }
        }
        return false;
    }

    private float match(final Tree<T> u, final Tree<T> v, final Table<T> D,
                        final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        float score = 0f;
        for (Tree<T> a : As) {
            final int A_ = A & ~(a.key);
            for (Tree<T> b : Bs) {
                score = Math.max(score, D.get(A_, B & ~(b.key)) + D.get(a.key, b.key));
            }
        }
        return score;
    }

    private float deleteLeft(final Tree<T> u, final Tree<T> v, final Table<T> D,
                             final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        float score = 0f;
        final int[] B_subsets = getSubsets(B);
        for (int i = 0; i < B_subsets.length; ++i) {
            final int B_ = B_subsets[i];
            final int BB = B & ~(B_);
            for (Tree<T> a : As) {
                if (a.degree() == 0) continue;
                score = Math.max(score, tables.get(a.index, v.index).get(Set.of(a.children()), B_) +
                        D.get(A & ~(a.key), BB) + scoring.deleteLeft(a.label));
            }
        }
        return score;
    }

    private float deleteRight(final Tree<T> u, final Tree<T> v, final Table<T> D,
                              final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        float score = 0f;
        final int[] A_subsets = getSubsets(A);
        for (int i = 0; i < A_subsets.length; ++i) {
            final int A_ = A_subsets[i];
            final int AA = A & ~(A_);
            for (Tree<T> b : Bs) {
                if (b.degree() == 0) continue;
                score = Math.max(score, tables.get(u.index, b.index).get(A_, Set.of(b.children())) +
                        D.get(AA, B & ~(b.key)) + scoring.deleteRight(b.label));
            }
        }
        return score;
    }

    private float preJoinLeft(final Tree<T> u, final Tree<T> v, final Table<T> D,
                              final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        float score = 0f;
        for (Tree<T> a : As) {
            final int A_ = A & ~(a.key);
            for (Tree<T> b : Bs) {
                assert !u.isRoot() : "u is not a root node";
                score = Math.max(score, D.getPrejoinLeft(A_, B & ~(b.key)) + D.getPrejoinLeft(a.key, b.key));
            }
        }
        return score;
    }

    private float preJoinRight(final Tree<T> u, final Tree<T> v, final Table<T> D,
                               final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        float score = 0f;
        for (Tree<T> a : As) {
            final int A_ = A & ~(a.key);
            for (Tree<T> b : Bs) {
                assert !v.isRoot() : "v is not a root node";
                score = Math.max(score, D.getPrejoinRight(A_, B & ~(b.key)) + D.getPrejoinRight(a.key, b.key));
            }
        }
        return score;
    }

    private float joinLeft(final Tree<T> u, final Tree<T> v, final Table<T> D,
                           final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        float score = 0f;
        final int[] B_subsets = getSubsets(B);
        for (Tree<T> a : As) {
            if (a.degree() == 0) continue;
            final Table<T> T = tables.get(a.index, v.index);
            final int setofAChilds = Set.of(a.children());
            final int withouta = A & ~(a.key);
            for (int i = 1; i < B_subsets.length; ++i) {
                final int B_ = B_subsets[i];
                score = Math.max(score, T.getPrejoinLeft(setofAChilds, B_) +
                        D.get(withouta, B & ~(B_)));
            }
        }
        return score;
    }

    private float joinRight(final Tree<T> u, final Tree<T> v, final Table<T> D,
                            final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        float score = 0f;
        final int[] A_subsets = getSubsets(A);
        for (Tree<T> b : Bs) {
            if (b.degree() == 0) continue;
            final int withoutb = B & ~(b.key);
            final int setofBChilds = Set.of(b.children());
            final Table<T> T = tables.get(u.index, b.index);
            for (int i = 1; i < A_subsets.length; ++i) {
                final int A_ = A_subsets[i];
                score = Math.max(score, T.getPrejoinRight(A_, setofBChilds) +
                        D.get(A & ~(A_), withoutb));
            }
        }
        return score;
    }

    private int[] getSubsets(int A) {
        return supersets[A];
    }

}
