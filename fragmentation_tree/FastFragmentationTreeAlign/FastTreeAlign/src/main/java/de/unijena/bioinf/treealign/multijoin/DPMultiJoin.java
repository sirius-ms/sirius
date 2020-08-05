
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

package de.unijena.bioinf.treealign.multijoin;

import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;
import de.unijena.bioinf.graphUtils.tree.TreeAdapter;
import de.unijena.bioinf.graphUtils.tree.TreeCursor;
import de.unijena.bioinf.treealign.*;
import de.unijena.bioinf.treealign.map.IntFloatIterator;
import de.unijena.bioinf.treealign.map.IntPairFloatIterator;
import de.unijena.bioinf.treealign.map.IntPairFloatMap;
import de.unijena.bioinf.treealign.scoring.Scoring;
import de.unijena.bioinf.treealign.sparse.QueueItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static de.unijena.bioinf.treealign.Set.of;
import static de.unijena.bioinf.treealign.Set.subList;
import static de.unijena.bioinf.treealign.map.IntPairFloatMap.ReturnType.LOWER;
import static de.unijena.bioinf.treealign.map.IntPairFloatMap.ReturnType.NOT_EXIST;

/**
 * @author Kai Dührkop
 */
public class DPMultiJoin<T> implements TreeAlignmentAlgorithm<T> {

    private final Scoring<T> scoring;
    private final TreeAdapter<T> adapter;
    private final Tree<T> left;
    private final Tree<T> right;
    private final ArrayList<Tree<T>> leftVertices;
    private final ArrayList<Tree<T>> rightVertices;
    private final ArrayList<Tree<T>> leftLeafs;
    private final ArrayList<Tree<T>> rightLeafs;
    private final int maxDegree;
    private final ArrayDeque<MultiJoinTraceItem<T>> traceQueue;
    private final ArrayDeque<QueueItem>[] queues;
    private final short numberOfJoins;
    private final TreeHashMap tables;
    private float optScore;
    private Tree<T> optLeft;
    private Tree<T> optRight;
    private Backtrace<T> tracer;
    private boolean scoreRoot;

    public DPMultiJoin(Scoring<T> scoring, int numberOfJoins, T left, T right, TreeAdapter<T> adapter) {
        if (numberOfJoins < 0 || numberOfJoins > Short.MAX_VALUE) {
            throw new IllegalArgumentException("illegal value for numberOfJoins: " + numberOfJoins);
        }
        this.numberOfJoins = (short) numberOfJoins;
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
        //this.tables = new TreeHashMap<T>(leftVertices.size(), rightVertices.size());
        this.queues = new ArrayDeque[leftDeco.maxDegree + rightDeco.maxDegree];
        for (int i = 0; i < queues.length; ++i) {
            queues[i] = new ArrayDeque<QueueItem>();
        }
        this.optScore = -1;
        this.optLeft = null;
        this.optRight = null;
        this.traceQueue = new ArrayDeque<MultiJoinTraceItem<T>>();
        this.tables = new TreeHashMap<T>(this.numberOfJoins, leftSize, rightSize);
        this.tracer = null;
        this.scoreRoot = false;
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

    private void clearQueues() {
        for (ArrayDeque<QueueItem> queue : queues) queue.clear();
    }

    @Override
    public float compute() {
        float opt = 0f;
        for (int i = 0; i < leftVertices.size(); ++i) {
            final Tree<T> u = leftVertices.get(i);
            final short L = (short) Math.min(u.getDepth(), numberOfJoins); // max number of left joins
            assert (u.getDepth() == 0 ? u.isRoot() : !u.isRoot()) : "" + u.getDepth() + " but u is root(" + u.isRoot() + ")";
            for (int j = 0; j < rightVertices.size(); ++j) {
                final Tree<T> v = rightVertices.get(j);
                assert (v.getDepth() == 0 ? v.isRoot() : !v.isRoot()) : "" + v.getDepth() + " but v is root(" + v.isRoot() + ")";
                final short R = (short) Math.min(v.getDepth(), numberOfJoins); // max number of right joins
                final HashTable<T> D = new HashTable<T>(u.children(), v.children(), L, R);
                tables.set(u.index, v.index, D);
                /*
                    PREJOIN-LOOP
                 */
                for (short l = 0; l <= L; ++l) {
                    for (short r = 0; r <= R; ++r) {
                        if (l == 0 && r == 0) continue;
                        clearQueues();
                        pushPairwisePreJoins(u, v, l, r, D);
                        for (int q = 0; q < queues.length; ++q) {
                            final ArrayDeque<QueueItem> queue = queues[q];
                            while (!queue.isEmpty()) {
                                final QueueItem item = queue.poll();
                                final List<Tree<T>> As = subList(u.children(), of(u.children()) & ~item.A);
                                final List<Tree<T>> Bs = subList(v.children(), of(v.children()) & ~item.B);
                                pushPreJoin(u, v, l, r, D, item.A, item.B, As, Bs, D.getJoin(l, r, item.A, item.B));
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
                if (numberOfJoins > 0) {
                    pushJoin(u, v, D, 0, 0, u.children(), v.children(), 0f);
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
                            if (numberOfJoins > 0) {
                                pushJoin(u, v, D, item.A, item.B, As, Bs, value);
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
        else return optScore;
    }

    private float vertexScore(final HashTable<T> D, Tree<T> u, Tree<T> v) {
        return D.getScore();
    }

    private void pushPreJoin(Tree<T> u, Tree<T> v, short l, short r, HashTable<T> D, int A, int B, List<Tree<T>> As,
                             List<Tree<T>> Bs, float value) {
        assert l + r != 0;
        final int asize = Integer.bitCount(A);
        final int bsize = Integer.bitCount(B);
        // suche in B nach einem Knoten, den a direkt matchen kann
        for (final Tree<T> a : As) {
            final int A_ = A | a.key;
            final int cardA = asize + 1;
            for (final Tree<T> b : Bs) {
                final float score = D.getJoin(l, r, a.key, b.key) + value;
                if (score > value) {
                    final int B_ = B | b.key;
                    final int cardB = bsize + 1;
                    switch (D.putJoinIfGreater(l, r, A_, B_, score)) {
                        case NOT_EXIST:
                            queues[cardA + cardB - 1].offer(new QueueItem(A_, B_));
                        case GREATER:
                            if (l > 0) D.putMaxJoinLeftIfGreater(l, r, B_, score);
                            if (r > 0) D.putMaxJoinRightIfGreater(l, r, A_, score);
                        case LOWER:
                    }
                }
            }
        }
        // betrachte die Teilmengen von B, die die Nachkommen von a matchen
        if (l + 1 <= numberOfJoins) {
            for (final Tree<T> a : As) {
                final int A_ = A | a.key;
                final int cardA = asize + 1;
                final HashTable<T> S = tables.get(a.index, v.index);
                final IntFloatIterator iter = S.eachInMaxJoinLeft((short) (l + 1), r);
                while (iter.hasNext()) {
                    iter.next();
                    if ((iter.getKey() & B) == 0) {
                        final int B_ = B | iter.getKey();
                        final int cardB = Integer.bitCount(B_);
                        final float score = value + iter.getValue();
                        switch (D.putJoinIfGreater(l, r, A_, B_, score)) {
                            case NOT_EXIST:
                                queues[cardA + cardB - 1].offer(new QueueItem(A_, B_));
                            case GREATER:
                                if (l > 0) D.putMaxJoinLeftIfGreater(l, r, B_, score);
                                if (r > 0) D.putMaxJoinRightIfGreater(l, r, A_, score);
                            case LOWER:
                        }
                    }
                }
            }
        }
        // betrachte die Teilmengen von A, die die Nachkommen von b matchen
        if (r + 1 <= numberOfJoins) {
            for (final Tree<T> b : Bs) {
                final int B_ = B | b.key;
                final int cardB = bsize + 1;
                // betrachte die Teilmengen von B, die die Nachkommen von a matchen
                final HashTable<T> S = tables.get(u.index, b.index);
                final IntFloatIterator iter = S.eachInMaxJoinRight(l, (short) (r + 1));
                while (iter.hasNext()) {
                    iter.next();
                    if ((iter.getKey() & A) == 0) {
                        final int A_ = A | iter.getKey();
                        final int cardA = Integer.bitCount(A_);
                        final float score = value + iter.getValue();
                        switch (D.putJoinIfGreater(l, r, A_, B_, score)) {
                            case NOT_EXIST:
                                queues[cardA + cardB - 1].offer(new QueueItem(A_, B_));
                            case GREATER:
                                if (l > 0) D.putMaxJoinLeftIfGreater(l, r, B_, score);
                                if (r > 0) D.putMaxJoinRightIfGreater(l, r, A_, score);
                            case LOWER:
                        }
                    }
                }
            }
        }
    }

    private void pushPairwisePreJoins(Tree<T> u, Tree<T> v, short l, short r, HashTable<T> D) {
        assert !(u.isRoot() && v.isRoot()) && l <= numberOfJoins && r <= numberOfJoins;
        // füge erst alle Joins hinzu, die entstehen wenn man die Kinder von u und v direkt joint
        for (Tree<T> a : u.children()) {
            for (Tree<T> b : v.children()) {
                final float score = scoring.join(a.eachAncestors(l), b.eachAncestors(r), l, r) +
                        tables.get(a.index, b.index).getScore();
                if (score > 0) {
                    D.setJoin(l, r, a.key, b.key, score);
                    if (l > 0) D.putMaxJoinLeftIfGreater(l, r, b.key, score);
                    if (r > 0) D.putMaxJoinRightIfGreater(l, r, a.key, score);
                    queues[1].offer(new QueueItem(a.key, b.key));
                }
            }
        }
        // füge außerdem alle Joins hinzu, die entstehen wenn man Nachkommen von u oder v joint
        if (l + 1 <= numberOfJoins) {
            for (Tree<T> a : u.children()) {
                final HashTable<T> S = tables.get(a.index, v.index);
                final IntFloatIterator iter = S.eachInMaxJoinLeft((short) (l + 1), r);
                while (iter.hasNext()) {
                    iter.next();
                    switch (D.putJoinIfGreater(l, r, a.key, iter.getKey(), iter.getValue())) {
                        case NOT_EXIST:
                            queues[Integer.bitCount(iter.getKey())].offer(new QueueItem(a.key, iter.getKey()));
                        case GREATER:
                            if (l > 0) D.putMaxJoinLeftIfGreater(l, r, iter.getKey(), iter.getValue());
                            if (r > 0) D.putMaxJoinRightIfGreater(l, r, a.key, iter.getValue());
                        case LOWER:
                    }
                }
            }
        }
        if (r + 1 <= numberOfJoins) {
            for (Tree<T> b : v.children()) {
                final HashTable<T> S = tables.get(u.index, b.index);
                final IntFloatIterator iter = S.eachInMaxJoinRight(l, (short) (r + 1));
                while (iter.hasNext()) {
                    iter.next();
                    switch (D.putJoinIfGreater(l, r, iter.getKey(), b.key, iter.getValue())) {
                        case NOT_EXIST:
                            queues[Integer.bitCount(iter.getKey())].offer(new QueueItem(iter.getKey(), b.key));
                        case GREATER:
                            if (r > 0) D.putMaxJoinRightIfGreater(l, r, iter.getKey(), iter.getValue());
                            if (l > 0) D.putMaxJoinLeftIfGreater(l, r, b.key, iter.getValue());
                        case LOWER:
                    }
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
        final IntPairFloatMap.ReturnType inserted = D.putIfGreater(A, B, score);
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

    private void pushJoin(Tree<T> u, Tree<T> v, HashTable<T> D, int A, int B, List<Tree<T>> As, List<Tree<T>> Bs, float value) {
        for (Tree<T> a : As) {
            final HashTable<T> T = tables.get(a.index, v.index);
            final IntFloatIterator iter = T.eachInMaxJoinLeft((short) 1, (short) 0);
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
        for (Tree<T> b : Bs) {
            final HashTable<T> T = tables.get(u.index, b.index);
            final IntFloatIterator iter = T.eachInMaxJoinRight((short) 0, (short) 1);
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

    /*
    *****************************************************************
    ************************** BACKTRACING **************************
    *****************************************************************
     */

    @Override
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
        //addTraceItemFor(left, right, optScore);
        while (!traceQueue.isEmpty()) {
            final MultiJoinTraceItem<T> item = traceQueue.poll();
            final List<Tree<T>> As = subList(item.u.children(), item.A);
            final List<Tree<T>> Bs = subList(item.v.children(), item.B);
            final HashTable<T> D = tables.get(item.u.index, item.v.index);
            final float opt = (item.l == 0 && item.r == 0) ? D.get(item.A, item.B)
                    : D.getJoin(item.l, item.r, item.A, item.B);
            if (opt == 0) continue;
            final boolean found;
            if (item.l == 0 && item.r == 0) {
                found = traceMatch(item.u, item.v, opt, D, item.A, item.B, As, Bs) ||
                        traceDeleteLeft(item.u, item.v, opt, D, item.A, item.B, As, Bs) ||
                        traceDeleteRight(item.u, item.v, opt, D, item.A, item.B, As, Bs) ||
                        traceMultiJoin(item.u, item.v, opt, D, item.A, item.B, As, Bs, item.l, item.r);
            } else {
                found = traceMultiJoin(item.u, item.v, opt, D, item.A, item.B, As, Bs, item.l, item.r);
            }
            assert found;
        }
    }

    private boolean addTraceItemFor(Tree<T> u, Tree<T> v, float opt) {
        return addTraceItemFor(u, v, Set.of(u.children()), Set.of(v.children()), opt);
    }

    private boolean addTraceItemFor(Tree<T> u, Tree<T> v, int A, int B, float opt) {
        final HashTable<T> D = tables.get(u.index, v.index);
        final IntPairFloatIterator iter = D.each();
        while (iter.hasNext()) {
            iter.next();
            if (iter.getValue() >= opt) {
                if ((iter.getLeft() & A) == iter.getLeft() && (iter.getRight() & B) == iter.getRight()) {
                    assert iter.getValue() == opt;
                    traceQueue.offerFirst(new MultiJoinTraceItem<T>(u, v, iter.getLeft(), iter.getRight()));
                    return true;
                }
            }
        }
        assert false;
        return false;
    }

    private boolean addTraceItemFor(Tree<T> u, Tree<T> v, short l, short r, int A, int B, float opt) {
        final HashTable<T> D = tables.get(u.index, v.index);
        final IntPairFloatIterator iter = D.eachInJoin(l, r);
        while (iter.hasNext()) {
            iter.next();
            if (iter.getValue() >= opt) {
                if ((iter.getLeft() & A) == iter.getLeft() && (iter.getRight() & B) == iter.getRight()) {
                    assert iter.getValue() == opt;
                    traceQueue.offerFirst(new MultiJoinTraceItem<T>(u, v, iter.getLeft(), iter.getRight(), l, r));
                    return true;
                }
            }
        }
        assert false;
        return false;
    }

    private boolean traceMultiJoin(final Tree<T> u, final Tree<T> v, final float opt, final HashTable<T> D,
                                   int A, int B, final List<Tree<T>> As, final List<Tree<T>> Bs,
                                   final short l, final short r) {
        float optScore = opt;
        // okay, ein Join kann aus drei Richtungen kommen:
        // 1. Direkter Join von Kindern von u und v
        if (l > 0 || r > 0) {
            outerLoop:
            for (final Tree<T> a : As) {
                for (final Tree<T> b : Bs) {
                    if ((b.key & B) != b.key) continue;
                    final float score = D.getJoin(l, r, a.key, b.key);
                    final float restScore = D.getJoin(l, r, A & ~a.key, B & ~b.key);
                    if (score + restScore >= optScore) {
                        // ist der Wert tatsächlich durch einen direkten Join zustande gekommen, oder wurde
                        // er aus einem früheren Tabelleneintrag übernommen?
                        if ((a.degree() > 0 && (l < numberOfJoins) && tables.get(a.index, v.index).getMaxJoinLeft((short) (l + 1), r, b.key) == score) ||
                                (b.degree() > 0 && (r < numberOfJoins) && tables.get(u.index, b.index).getMaxJoinRight(l, (short) (r + 1), a.key) == score)) {
                            continue;
                        }
                        final float subtreeScore = tables.get(a.index, b.index).getScore();
                        optScore -= score;
                        assert Math.abs(optScore - restScore) < 1e-6;
                        tracer.join(score - subtreeScore, a.eachAncestors(l), b.eachAncestors(r), l, r);
                        if (subtreeScore > 0) {
                            addTraceItemFor(a, b, subtreeScore);
                        }
                        if (restScore == 0) {
                            return true;
                        }
                        A = A & ~a.key;
                        B = B & ~b.key;
                        continue outerLoop;
                    }
                }
            }
        }
        // 2. Join von Nachkommen eines Kindes von u mit Kindern von v
        if (l + 1 <= numberOfJoins) {
            outerloop:
            for (final Tree<T> a : As) {
                if ((a.key & A) != a.key) continue;
                final HashTable<T> S = tables.get(a.index, v.index);
                final IntFloatIterator iter = S.eachInMaxJoinLeft((short) (l + 1), r);
                while (iter.hasNext()) {
                    iter.next();
                    if ((iter.getKey() & B) == iter.getKey()) {
                        final float score = iter.getValue();
                        final float restScore = (l == 0 && r == 0) ? D.get(A & ~a.key, B & ~iter.getKey())
                                : D.getJoin(l, r, A & ~a.key, B & ~iter.getKey());
                        if (score + restScore >= optScore) {
                            optScore -= score;
                            assert optScore == restScore;
                            tracer.innerJoinLeft(a.label);
                            addTraceItemFor(a, v, (short) (l + 1), r, of(a.children()), iter.getKey(), score);
                            if (restScore == 0) {
                                return true;
                            } else {
                                A = A & ~a.key;
                                B = B & ~iter.getKey();
                                continue outerloop;
                            }
                        }
                    }
                }
            }
        }
        // 3. Join von Nachkommen eines Kindes von v mit Kindern von u
        if (r + 1 <= numberOfJoins) {
            outerloop:
            for (final Tree<T> b : Bs) {
                if ((b.key & B) != b.key) continue;
                final HashTable<T> S = tables.get(u.index, b.index);
                final IntFloatIterator iter = S.eachInMaxJoinRight(l, (short) (r + 1));
                while (iter.hasNext()) {
                    iter.next();
                    if ((iter.getKey() & A) == iter.getKey()) {
                        final float score = iter.getValue();
                        final float restScore = (l == 0 && r == 0) ? D.get(A & ~iter.getKey(), B & ~b.key)
                                : D.getJoin(l, r, A & ~iter.getKey(), B & ~b.key);
                        if (score + restScore >= optScore) {
                            optScore -= score;
                            assert optScore == restScore;
                            tracer.innerJoinRight(b.label);
                            addTraceItemFor(u, b, l, (short) (r + 1), iter.getKey(), of(b.children()), score);
                            A = A & ~iter.getKey();
                            B = B & ~b.key;
                            if (optScore == 0) {
                                return true;
                            } else {
                                continue outerloop;
                            }
                        }
                    }
                }
            }
        }

        // im Join-Fall (l=r=0) darf der Score auch von Matches und Deletes erzeugt werden
        if (l == 0 && r == 0 && optScore < opt) {
            return addTraceItemFor(u, v, A, B, optScore);
        }

        return optScore == 0;
    }

    private boolean traceMatch(Tree<T> u, Tree<T> v, float opt, final HashTable<T> D,
                               final int A, final int B, final List<Tree<T>> As, final List<Tree<T>> Bs) {
        for (Tree<T> a : As) {
            final int A_ = A & ~a.key;
            for (Tree<T> b : Bs) {
                final int B_ = B & ~b.key;
                final float pre = /*(A_ == 0 || B_ == 0) ? 0 :*/ D.get(A_, B_);
                //if (A_ > 0 && B_ > 0 && pre == 0) continue;
                final float matchScore = scoring.match(a.label, b.label);
                final float subtreeScore = tables.get(a.index, b.index).getScore();
                float score = subtreeScore + pre + matchScore;
                if (score >= opt) {
                    assert Math.abs(score - opt) < 1e-6 : score + " from match is greater than opt: " + opt;
                    tracer.match(matchScore, a.label, b.label);
                    if (pre > 0) traceQueue.offerFirst(new MultiJoinTraceItem<T>(u, v, A_, B_));
                    if (a.degree() > 0 && b.degree() > 0 && subtreeScore > 0) {
                        addTraceItemFor(a, b, subtreeScore);
                    }
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
                    final float pre = /*(A_ == 0 || BB == 0) ? 0 : */ D.get(A_, BB);
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
                    final float pre = /*(AA == 0 || B_ == 0) ? 0 : */ D.get(AA, B_);
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


}
