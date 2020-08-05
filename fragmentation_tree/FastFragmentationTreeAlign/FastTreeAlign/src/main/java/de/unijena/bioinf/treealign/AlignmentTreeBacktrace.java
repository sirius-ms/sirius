
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

package de.unijena.bioinf.treealign;

import de.unijena.bioinf.graphUtils.tree.TreeAdapter;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by kaidu on 05.08.14.
 */
public class AlignmentTreeBacktrace<T> implements Backtrace<T> {

    private final AlignmentTree<T> tree;

    public AlignmentTreeBacktrace(TreeAdapter<T> adapter) {
        tree = new AlignmentTree<T>(adapter);
    }

    public AlignmentTree<T> getAlignmentTree() {
        tree.assignAll();
        return tree;
    }

    @Override
    public void deleteLeft(float score, T node) {
        if (tree.getRoot() != null)
            tree.addDeletionLeft(node, score);
    }

    @Override
    public void deleteRight(float score, T node) {
        if (tree.getRoot() != null)
            tree.addDeletionRight(node, score);
    }

    @Override
    public void match(float score, T left, T right) {
        if (tree.getRoot() == null) tree.setRoot(left, right, score);
        else tree.addMatch(left, right, score);
    }

    @Override
    public void innerJoinLeft(T node) {
        tree.addInnerJoinLeft(node);
    }

    @Override
    public void innerJoinRight(T node) {
        tree.addInnerJoinRight(node);
    }

    @Override
    public void join(float score, Iterator<T> left, Iterator<T> right, int leftNumber, int rightNumber) {
        ///*
        final ArrayList<T> leftS = new ArrayList<T>();
        while (left.hasNext()) leftS.add(left.next());
        final ArrayList<T> rightS = new ArrayList<T>();
        while (right.hasNext()) rightS.add(right.next());
        System.out.println("JOIN");
        System.out.println(leftS);
        System.out.println(rightS);
        System.out.println("--> " + score);
        System.out.flush();
        System.err.flush();
        tree.addJoin(leftS.iterator(), rightS.iterator(), leftNumber, rightNumber, score);
        //*/
        //tree.addJoin(left, right, leftNumber, rightNumber, score);
    }

    @Override
    public void matchVertices(float score, T left, T right) {
        if (tree.getRoot() == null) tree.setRoot(left, right, score);
        else tree.addMatch(left, right, score);
    }
}
