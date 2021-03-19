
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

package de.unijena.bioinf.counting;

import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;
import de.unijena.bioinf.graphUtils.tree.TreeAdapter;
import de.unijena.bioinf.graphUtils.tree.TreeCursor;
import de.unijena.bioinf.treealign.Tree;
import de.unijena.bioinf.treealign.TreeDecorator;
import de.unijena.bioinf.treealign.scoring.SimpleEqualityScoring;

import java.util.ArrayList;

public class DPPathCounting<T> {

    protected final long[][] D;
    private final ArrayList<Tree<T>> leftVertices;
    private final ArrayList<Tree<T>> rightVertices;
    private final ArrayList<Tree<T>> leftLeafs;
    private final ArrayList<Tree<T>> rightLeafs;
    private final TreeAdapter<T> adapter;
    private final Tree<T> left;
    private final Tree<T> right;
    private final SimpleEqualityScoring<T> scoring;

    public DPPathCounting(SimpleEqualityScoring<T> scoring, T left, T right, TreeAdapter<T> adapter) {
        this.adapter = adapter;
        final int leftSize = TreeCursor.getCursor(left, adapter).numberOfVertices();
        final int rightSize = TreeCursor.getCursor(right, adapter).numberOfVertices();
        this.leftVertices = new ArrayList<Tree<T>>(leftSize);
        this.rightVertices = new ArrayList<Tree<T>>(rightSize);
        this.leftLeafs = new ArrayList<Tree<T>>(leftSize);
        this.rightLeafs = new ArrayList<Tree<T>>(rightSize);
        final TreeDecorator<T> leftDeco = new TreeDecorator<T>(leftVertices, leftLeafs);
        final TreeDecorator<T> rightDeco = new TreeDecorator<T>(rightVertices, rightLeafs);
        this.left = new PostOrderTraversal<T>(left, adapter).call(leftDeco);
        this.right = new PostOrderTraversal<T>(right, adapter).call(rightDeco);
        this.D = new long[leftVertices.size()][rightVertices.size()];
        this.scoring = scoring;
    }

    protected long recurrence(long counter, int a, int b) {
        return counter + 1 + ((a < 0 || b < 0) ? 0 : D[a][b]);
    }

    public long compute() {
        long sum = 0l;
        for (int i = 0; i < leftVertices.size(); ++i) {
            final Tree<T> u = leftVertices.get(i);
            for (int j = 0; j < rightVertices.size(); ++j) {
                final Tree<T> v = rightVertices.get(j);

                // primitive way to do: iterate each edges
                long counter = 1;
                for (Tree<T> a : u.children()) {
                    for (Tree<T> b : v.children()) {
                        if (scoring.isMatching(a.label, b.label)) {
                            counter = recurrence(counter, a.index, b.index);
                        }
                    }
                }
                --counter;       // -_- wie wäre es mit Initialisierung mit 0?
                D[u.index][v.index] = counter;

                sum += counter;
            }
        }
        return sum;
    }
}
