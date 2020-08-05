
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

package de.unijena.bioinf.treealign.sparse;


import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;
import de.unijena.bioinf.graphUtils.tree.TreeAdapter;
import de.unijena.bioinf.graphUtils.tree.TreeCursor;
import de.unijena.bioinf.treealign.*;
import de.unijena.bioinf.treealign.map.IntFloatIterator;
import de.unijena.bioinf.treealign.map.IntPairFloatIterator;
import de.unijena.bioinf.treealign.scoring.Scoring;
import de.unijena.bioinf.util.Iterators;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static de.unijena.bioinf.treealign.Set.subList;
import static de.unijena.bioinf.treealign.map.IntPairFloatMap.ReturnType;
import static de.unijena.bioinf.treealign.map.IntPairFloatMap.ReturnType.LOWER;
import static de.unijena.bioinf.treealign.map.IntPairFloatMap.ReturnType.NOT_EXIST;

public class DPSparseTreeAlign<T> implements TreeAlignmentAlgorithm<T> {

    private final TreeAdapter<T> adapter;
    private final Scoring<T> scoring;
    private final Tree<T> left;
    private final Tree<T> right;
    private final ArrayList<Tree<T>> leftVertices;
    private final ArrayList<Tree<T>> rightVertices;
    private final ArrayList<Tree<T>> leftLeafs;
    private final ArrayList<Tree<T>> rightLeafs;
    private final int maxDegree;
    private final TreeHashMap<T> tables;
    private final ArrayDeque<QueueItem>[] queues;
    private final ArrayDeque<TraceItem<T>> traceQueue;
    private final boolean useJoins;
    private float optScore;
    private Tree<T> optLeft;
    private Tree<T> optRight;
    private Backtrace<T> tracer;
    private boolean scoreRoot;

    public DPSparseTreeAlign(Scoring<T> scoring, boolean useJoins, T left, T right, TreeAdapter<T> adapter) {
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
        this.maxDegree = Math.max(leftDeco.maxDegree, rightDeco.maxDegree);
        this.tables = new TreeHashMap<T>(leftVertices.size(), rightVertices.size());
        this.queues = new ArrayDeque[leftDeco.maxDegree + rightDeco.maxDegree];
        for (int i = 0; i < queues.length; ++i) {
            queues[i] = new ArrayDeque<QueueItem>();
        }
        this.optScore = -1;
        this.optLeft = null;
        this.optRight = null;
        this.traceQueue = new ArrayDeque<TraceItem<T>>();
        this.tracer = null;
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
                final HashTable<T> table = tables.get(a.index, b.index);
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
        float opt = 0f;
        for (int i = 0; i < leftVertices.size(); ++i) {
            final Tree<T> u = leftVertices.get(i);
            for (int j = 0; j < rightVertices.size(); ++j) {
                final Tree<T> v = rightVertices.get(j);
                final HashTable<T> D = new HashTable<T>(u.children(), v.children(), useJoins);
                tables.set(u.index, v.index, D);
                /*
                    PREJOIN-LOOP
                 */
                if (useJoins) {
                    clearQueues();
                    if (!u.isRoot()) {
                        pushPairwisePreJoinsLeft(u, v, D);
                        for (int q = 0; q < queues.length; ++q) {
                            final ArrayDeque<QueueItem> queue = queues[q];
                            while (!queue.isEmpty()) {
                                final QueueItem item = queue.poll();
                                final List<Tree<T>> As = subList(u.children(), ((1 << u.degree()) - 1) & ~item.A);
                                final List<Tree<T>> Bs = subList(v.children(), ((1 << v.degree()) - 1) & ~item.B);
                                pushPreJoinLeft(u, v, D, item.A, item.B, As, Bs);
                            }
                        }
                    }
                    clearQueues();
                    if (!v.isRoot()) {
                        pushPairwisePreJoinsRight(u, v, D);
                        for (int q = 0; q < queues.length; ++q) {
                            final ArrayDeque<QueueItem> queue = queues[q];
                            while (!queue.isEmpty()) {
                                final QueueItem item = queue.poll();
                                final List<Tree<T>> As = subList(u.children(), ((1 << u.degree()) - 1) & ~item.A);
                                final List<Tree<T>> Bs = subList(v.children(), ((1 << v.degree()) - 1) & ~item.B);
                                pushPreJoinRight(u, v, D, item.A, item.B, As, Bs);
                            }
                        }
                    }
                }
                /*
                    ALIGNMENT-LOOP
                 */
                clearQueues();
                pushPairwiseMatches(u, v, D);
                pushDeleteLeft(u, v, D, 0, 0, u.children(), v.children(), 0f);
                pushDeleteRight(u, v, D, 0, 0, u.children(), v.children(), 0f);
                if (useJoins) {
                    pushJoinLeft(u, v, D, 0, 0, u.children(), v.children(), 0f);
                    pushJoinRight(u, v, D, 0, 0, u.children(), v.children(), 0f);
                }
                for (int q = 0; q < queues.length; ++q) {
                    final ArrayDeque<QueueItem> queue = queues[q];
                    while (!queue.isEmpty()) {
                        final QueueItem item = queue.poll();
                        final List<Tree<T>> As = subList(u.children(), ((1 << u.degree()) - 1) & ~item.A);
                        final List<Tree<T>> Bs = subList(v.children(), ((1 << v.degree()) - 1) & ~item.B);
                        final float value = D.get(item.A, item.B);
                        assert value > 0;
                        pushDeleteLeft(u, v, D, item.A, item.B, As, Bs, value);
                        pushDeleteRight(u, v, D, item.A, item.B, As, Bs, value);
                        if (q > 0) {
                            pushMatch(u, v, D, item.A, item.B, As, Bs, value);
                            if (useJoins) {
                                pushJoinLeft(u, v, D, item.A, item.B, As, Bs, value);
                                pushJoinRight(u, v, D, item.A, item.B, As, Bs, value);
                            }
                        }
                    }
                }

                ///////////////////
                {
                    final float newScore = vertexScore(D, u, v);
                    if (newScore > opt) {
                        opt = newScore;
                        optLeft = u;
                        optRight = v;
                    }
                }
            }
        }
        this.optScore = opt;
        if (scoring.isScoringVertices()) return scoreSubtreeRoots();
        else return opt;
    }

    private float vertexScore(final HashTable<T> D, Tree<T> u, Tree<T> v) {
        return D.getScore();
    }

    private void pushJoinLeft(Tree<T> u, Tree<T> v, HashTable<T> D, int A, int B, List<Tree<T>> As, List<Tree<T>> Bs, float value) {
        for (Tree<T> a : As) {
            final HashTable<T> T = tables.get(a.index, v.index);
            final IntFloatIterator iter = T.eachInMaxJoinLeft();
            while (iter.hasNext()) {
                iter.next();
                int B_ = iter.getKey();
                if ((B_ & B) != 0) continue;
                final float score = value + iter.getValue();
                if (score > value) {
                    update(u, v, D, A | a.key, B | B_, score);
                }

            }
        }
    }

    private void pushJoinRight(Tree<T> u, Tree<T> v, HashTable<T> D, int A, int B, List<Tree<T>> As, List<Tree<T>> Bs, float value) {
        for (Tree<T> b : Bs) {
            final HashTable<T> T = tables.get(u.index, b.index);
            final IntFloatIterator iter = T.eachInMaxJoinRight();
            while (iter.hasNext()) {
                iter.next();
                int A_ = iter.getKey();
                if ((A_ & A) != 0) continue;

                final float score = value + iter.getValue();
                if (score > value) {
                    update(u, v, D, A | A_, B | b.key, score);
                }

            }
        }
    }

    private void pushPreJoinLeft(Tree<T> u, Tree<T> v, HashTable<T> D, int A, int B, List<Tree<T>> As, List<Tree<T>> Bs) {
        final float value = D.getJoinLeft(A, B);
        final int cardA = Integer.bitCount(A);
        final int cardB = Integer.bitCount(B);
        for (Tree<T> a : As) {
            for (Tree<T> b : Bs) {
                final float score = D.getJoinLeft(a.key, b.key) + value;
                if (score > value) {
                    final int A_ = A | a.key;
                    final int B_ = B | b.key;
                    final ReturnType inserted = D.putJoinLeftIfGreater(A_, B_, score);
                    if (inserted != LOWER) {
                        if (inserted == NOT_EXIST) {
                            queues[cardA + cardB + 1].offer(new QueueItem(A_, B_));
                        }
                        D.putMaxJoinLeftIfGreater(B_, score);
                    }
                }
            }
        }
    }

    private void pushPreJoinRight(Tree<T> u, Tree<T> v, HashTable<T> D, int A, int B, List<Tree<T>> As, List<Tree<T>> Bs) {
        final float value = D.getJoinRight(A, B);
        final int cardA = Integer.bitCount(A);
        final int cardB = Integer.bitCount(B);
        for (Tree<T> a : As) {
            for (Tree<T> b : Bs) {
                final float score = D.getJoinRight(a.key, b.key) + value;
                if (score > value) {
                    final int A_ = A | a.key;
                    final int B_ = B | b.key;
                    final ReturnType inserted = D.putJoinRightIfGreater(A_, B_, score);
                    if (inserted != LOWER) {
                        if (inserted == NOT_EXIST) {
                            queues[cardA + cardB + 1].offer(new QueueItem(A_, B_));
                        }
                        D.putMaxJoinRightIfGreater(A_, score);
                    }
                }
            }
        }
    }

    private void pushPairwisePreJoinsLeft(Tree<T> u, Tree<T> v, HashTable<T> D) {
        assert !u.isRoot();
        for (Tree<T> a : u.children()) {
            for (Tree<T> b : v.children()) {
                final float score = scoring.joinLeft(u.label, a.label, b.label) +
                        tables.get(a.index, b.index).getScore();
                if (score > 0) {
                    D.setJoinLeft(a.key, b.key, score);
                    D.putMaxJoinLeftIfGreater(b.key, score);
                    queues[1].offer(new QueueItem(a.key, b.key));
                }
            }
        }
    }

    private void pushPairwisePreJoinsRight(Tree<T> u, Tree<T> v, HashTable<T> D) {
        assert !v.isRoot();
        for (Tree<T> a : u.children()) {
            for (Tree<T> b : v.children()) {
                final float score = scoring.joinRight(v.label, b.label, a.label) +
                        tables.get(a.index, b.index).getScore();
                if (score > 0) {
                    D.setJoinRight(a.key, b.key, score);
                    D.putMaxJoinRightIfGreater(a.key, score);
                    queues[1].offer(new QueueItem(a.key, b.key));
                }
            }
        }
    }

    private void pushPairwiseMatches(Tree<T> u, Tree<T> v, HashTable<T> D) {
        for (int i = 0; i < u.degree(); ++i) {
            final Tree<T> a = u.children().get(i);
            for (int j = 0; j < v.degree(); ++j) {
                final Tree<T> b = v.children().get(j);
                final float score = scoring.match(a.label, b.label) + tables.get(a.index, b.index).getScore();
                if (score > 0) {
                    D.set(a.key, b.key, score);
                    D.putMaxLeftIfGreater(b.key, score);
                    D.putMaxRightIfGreater(a.key, score);
                    D.setScoreIfGreater(score);
                    assert D.get(a.key, b.key) > 0;
                    queues[1].offer(new QueueItem(a.key, b.key));
                }
            }
        }
    }

    private void update(Tree<T> u, Tree<T> v, HashTable<T> D, int A, int B, float score) {
        assert score > 0;
        final int cardA = Integer.bitCount(A);
        final int cardB = Integer.bitCount(B);
        final ReturnType inserted = D.putIfGreater(A, B, score);
        if (inserted != LOWER) {
            if (inserted == NOT_EXIST) {
                queues[cardA + cardB - 1].offer(new QueueItem(A, B));
                assert D.get(A, B) > 0;
            }
            D.putMaxLeftIfGreater(B, score);
            D.putMaxRightIfGreater(A, score);
            D.setScoreIfGreater(score);
        }
    }

    private void pushMatch(Tree<T> u, Tree<T> v, HashTable<T> D, int A, int B,
                           List<Tree<T>> As, List<Tree<T>> Bs, float value) {
        for (Tree<T> a : As) {
            for (Tree<T> b : Bs) {
                final float score = D.get(a.key, b.key) + value;
                if (score > value) {
                    update(u, v, D, A | a.key, B | b.key, score);
                }
            }
        }

    }

    private void pushDeleteLeft(Tree<T> u, Tree<T> v, HashTable<T> D, int A, int B,
                                List<Tree<T>> As, List<Tree<T>> Bs, float value) {
        for (Tree<T> a : As) {
            final float gapScore = value + scoring.deleteLeft(a.label);
            final HashTable<T> T = tables.get(a.index, v.index);
            final IntFloatIterator iter = T.eachInMaxLeft();
            while (iter.hasNext()) {
                iter.next();
                final int B_ = iter.getKey();
                if ((B & B_) == 0) {
                    final float newValue = gapScore + iter.getValue();
                    if (newValue > value) {
                        update(u, v, D, A | a.key, B | B_, newValue);
                    }
                }
            }
        }

    }

    private void pushDeleteRight(Tree<T> u, Tree<T> v, HashTable<T> D, int A, int B,
                                 List<Tree<T>> As, List<Tree<T>> Bs, float value) {
        for (Tree<T> b : Bs) {
            final float gapScore = value + scoring.deleteRight(b.label);
            final HashTable<T> T = tables.get(u.index, b.index);
            final IntFloatIterator iter = T.eachInMaxRight();
            while (iter.hasNext()) {
                iter.next();
                final int A_ = iter.getKey();
                if ((A & A_) == 0) {
                    final float newValue = gapScore + iter.getValue();
                    if (newValue > value) {
                        update(u, v, D, A | A_, B | b.key, newValue);
                    }
                }
            }
        }
    }

    private void clearQueues() {
        for (ArrayDeque<QueueItem> queue : queues) queue.clear();
    }

    /*

        Backtracking

     */

    public void backtrace(Backtrace<T> tracer) {
        if (optScore <= 0) return;
        traceQueue.clear();
        this.tracer = tracer;
        final float score;
        if (scoreRoot) {
            tracer.matchVertices(optScore - tables.get(optLeft.index, optRight.index).getScore(), optLeft.label, optRight.label);
            score = tables.get(optLeft.index, optRight.index).getScore();
        } else {
            score = optScore;
        }
        if (score <= 0) return;
        addTraceItemFor(optLeft, optRight, score);
        while (!traceQueue.isEmpty()) {
            final TraceItem<T> item = traceQueue.poll();
            final List<Tree<T>> As = subList(item.u.children(), item.A);
            final List<Tree<T>> Bs = subList(item.v.children(), item.B);
            final HashTable<T> D = tables.get(item.u.index, item.v.index);
            final float opt = D.get(item.A, item.B);
            if (opt == 0) continue;
            final boolean found = traceMatch(item.u, item.v, opt, D, item.A, item.B, As, Bs) ||
                    traceDeleteLeft(item.u, item.v, opt, D, item.A, item.B, As, Bs) ||
                    traceDeleteRight(item.u, item.v, opt, D, item.A, item.B, As, Bs) ||
                    (useJoins && (
                            traceJoinLeft(item.u, item.v, opt, D, item.A, item.B, As, Bs) ||
                                    traceJoinRight(item.u, item.v, opt, D, item.A, item.B, As, Bs)
                    ));
            assert found;
        }
    }

    private boolean addTraceItemFor(Tree<T> u, Tree<T> v, float opt) {
        return addTraceItemFor(u, v, Set.of(u.children()), Set.of(v.children()), opt);
    }

    private boolean addTraceItemFor(Tree<T> u, Tree<T> v, int A, int B, float opt) {
        final HashTable<T> D = tables.get(u.index, v.index);
        assert D.getScore() >= opt;
        final IntPairFloatIterator iter = D.each();
        while (iter.hasNext()) {
            iter.next();
            if (iter.getValue() >= opt) {
                if ((iter.getLeft() & A) == iter.getLeft() && (iter.getRight() & B) == iter.getRight()) {
                    traceQueue.offer(new TraceItem<T>(u, v, iter.getLeft(), iter.getRight()));
                    return true;
                }
            }
        }
        assert false;
        return false;
    }

    private boolean traceMatch(Tree<T> u, Tree<T> v, float opt, final HashTable<T> D,
                               final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        for (Tree<T> a : As) {
            final int A_ = A & ~a.key;
            for (Tree<T> b : Bs) {
                final int B_ = B & ~b.key;
                final float pre = D.get(A_, B_);
                //if (A_ > 0 && B_ > 0 && pre == 0) continue;
                final float matchScore = scoring.match(a.label, b.label);
                final float subtreeScore = tables.get(a.index, b.index).getScore();
                float score = subtreeScore + pre + matchScore;
                if (score >= opt) {
                    assert score == opt;
                    tracer.match(matchScore, a.label, b.label);
                    if (a.degree() > 0 && b.degree() > 0 && subtreeScore > 0) {
                        addTraceItemFor(a, b, subtreeScore);
                    }
                    if (pre > 0) traceQueue.offer(new TraceItem<T>(u, v, A_, B_));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean traceDeleteLeft(Tree<T> u, Tree<T> v, float opt, final HashTable<T> D,
                                    final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        for (Tree<T> a : As) {
            final int A_ = A & ~a.key;
            final IntFloatIterator iter = tables.get(a.index, v.index).eachInMaxLeft();
            while (iter.hasNext()) {
                iter.next();
                final int B_ = iter.getKey();
                if ((B & B_) == B_) {
                    final int BB = B & ~B_;
                    final float deleteScore = scoring.deleteLeft(a.label);
                    final float pre = D.get(A_, BB);
                    final float score = iter.getValue() + deleteScore + pre;
                    if (score >= opt) {
                        assert iter.getValue() > 0;
                        assert score == opt;
                        tracer.deleteLeft(deleteScore, a.label);
                        if (a.degree() > 0) {
                            addTraceItemFor(a, v, Set.of(a.children()), B_, iter.getValue());
                        }
                        if (pre > 0) {
                            addTraceItemFor(u, v, A_, BB, pre);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean traceDeleteRight(Tree<T> u, Tree<T> v, float opt, final HashTable<T> D,
                                     final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        for (Tree<T> b : Bs) {
            final int B_ = B & ~b.key;
            final IntFloatIterator iter = tables.get(u.index, b.index).eachInMaxRight();
            while (iter.hasNext()) {
                iter.next();
                final int A_ = iter.getKey();
                if ((A & A_) == A_) {
                    final int AA = A & ~A_;
                    final float deleteScore = scoring.deleteRight(b.label);
                    final float pre = D.get(AA, B_);
                    final float score = iter.getValue() + deleteScore + pre;
                    if (score >= opt) {
                        assert iter.getValue() > 0;
                        assert score == opt;
                        tracer.deleteRight(deleteScore, b.label);
                        if (b.degree() > 0) {
                            addTraceItemFor(u, b, A_, Set.of(b.children()), iter.getValue());
                        }
                        if (pre > 0) {
                            addTraceItemFor(u, v, AA, B_, pre);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean traceJoinLeft(Tree<T> u, Tree<T> v, float opt, final HashTable<T> D,
                                  final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        for (final Tree<T> a : As) {
            final int A__ = A & ~a.key;
            final HashTable<T> T = tables.get(a.index, v.index);
            final IntFloatIterator iter = T.eachInMaxJoinLeft();
            while (iter.hasNext()) {
                iter.next();
                final int B_ = iter.getKey();
                if ((B_ & B) == B_) {
                    final int B__ = B & ~B_;
                    final float pre = D.get(A__, B__);
                    final float value = iter.getValue();
                    final float score = value + pre;
                    if (score >= opt) {
                        assert score == opt;
                        // okay, ich weiß jetzt das a und eines (oder mehrere) seiner Kinder mit B_ per JOIN matchen
                        // jetzt gilt rauszufinden, woher genau der Join-Score zustande kommt, also welches der Kinder
                        // dafür verantwortlich ist. Da bisher nur Joins über 2 Kanten gehen, kann ich das direkt
                        // inlinen
                        final IntPairFloatIterator pairIter = T.eachInJoinLeft();
                        while (pairIter.hasNext()) {
                            pairIter.next();
                            final int X = pairIter.getLeft();
                            final int Y = pairIter.getRight();
                            // TRICK: Es reicht wenn ich IRGENDEINEN Eintrag in der Tabelle nehme, der diesen Score
                            // erzeugt. Wegen der Dominanzregel kann es keinen höheren Eintrag geben, der
                            // falsche Knoten enthält die keinen Score einbringen
                            if (/* TODO: experimental */ Y == B_ && pairIter.getValue() >= value) {
                                assert pairIter.getValue() == value;
                                final List<Tree<T>> Ys = Set.subList(v.children(), Y);
                                for (Tree<T> x : Set.subList(a.children(), X)) {
                                    for (Tree<T> y : Ys) {
                                        final float preScore = T.getJoinLeft(X & ~x.key, Y & ~y.key);
                                        if (T.getJoinLeft(x.key, y.key) + preScore >= value) {
                                            assert T.getJoinLeft(x.key, y.key) + preScore == value;
                                            final float joinScore = scoring.joinLeft(a.label, x.label, y.label);
                                            tracer.innerJoinLeft(a.label);
                                            tracer.join(joinScore, x.eachAncestors(1), Iterators.singleton(y.label), 2, 1);
                                            final float subtreeScore = value - joinScore - preScore;
                                            if (subtreeScore > 0) {
                                                addTraceItemFor(x, y, subtreeScore);
                                            }
                                        }
                                    }
                                }
                                if (pre > 0) addTraceItemFor(u, v, A__, B__, pre);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean traceJoinRight(Tree<T> u, Tree<T> v, float opt, final HashTable<T> D,
                                   final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        for (final Tree<T> b : Bs) {
            final int B_ = B & ~b.key;
            final HashTable<T> T = tables.get(u.index, b.index);
            final IntFloatIterator iter = T.eachInMaxJoinRight();
            while (iter.hasNext()) {
                iter.next();
                final int A_ = iter.getKey();
                if ((A_ & A) == A_) {
                    final int AA = A & ~A_;
                    final float pre = D.get(AA, B_);
                    final float value = iter.getValue();
                    final float jvalue = value;
                    final float score = jvalue + pre;
                    if (score >= opt) {
                        assert score == opt;
                        // okay, ich weiß jetzt das a und eines (oder mehrere) seiner Kinder mit B_ per JOIN matchen
                        // jetzt gilt rauszufinden, woher genau der Join-Score zustande kommt, also welches der Kinder
                        // dafür verantwortlich ist. Da bisher nur Joins über 2 Kanten gehen, kann ich das direkt
                        // inlinen
                        final IntPairFloatIterator pairIter = T.eachInJoinRight();
                        while (pairIter.hasNext()) {
                            pairIter.next();
                            final int X = pairIter.getLeft();
                            final int Y = pairIter.getRight();
                            // TRICK: Es reicht wenn ich IRGENDEINEN Eintrag in der Tabelle nehme, der diesen Score
                            // erzeugt. Wegen der Dominanzregel kann es keinen höheren Eintrag geben, der
                            // falsche Knoten enthält die keinen Score einbringen
                            if (/* TODO: experimental */ X == A_ && pairIter.getValue() >= value) {
                                assert pairIter.getValue() == value;
                                final List<Tree<T>> Xs = Set.subList(u.children(), X);
                                for (Tree<T> y : Set.subList(b.children(), Y)) {
                                    for (Tree<T> x : Xs) {
                                        final float preScore = T.getJoinRight(X & ~x.key, Y & ~y.key);
                                        if (T.getJoinRight(x.key, y.key) + preScore >= value) {
                                            assert T.getJoinRight(x.key, y.key) + preScore == value;
                                            tracer.innerJoinRight(b.label);
                                            final float joinScore = scoring.joinRight(b.label, y.label, x.label);
                                            tracer.join(joinScore, Iterators.singleton(x.label), y.eachAncestors(1),
                                                    1, 2);
                                            final float subtreeScore = jvalue - joinScore - preScore;
                                            if (subtreeScore > 0) {
                                                addTraceItemFor(x, y, subtreeScore);
                                            }
                                        }
                                    }
                                }
                                if (pre > 0) addTraceItemFor(u, v, AA, B_, pre);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }


}
