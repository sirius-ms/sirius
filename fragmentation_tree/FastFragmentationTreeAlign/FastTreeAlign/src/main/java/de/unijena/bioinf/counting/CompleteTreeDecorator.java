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
package de.unijena.bioinf.counting;

import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;
import de.unijena.bioinf.treealign.Tree;

import java.util.ArrayList;
import java.util.List;

public class CompleteTreeDecorator<T> extends PostOrderTraversal.Call<T, Tree<T>> {
    private final ArrayList<Tree<T>> list;
    public int maxDegree;
    private int index;

    public CompleteTreeDecorator(ArrayList<Tree<T>> list) {
        this.index = 0;
        this.list = list;
    }

    public Tree<T> call(T vertex, List<Tree<T>> values, boolean isRoot) {
        assert isRoot == (getCursor().getDepth() == 0);
        final int i = index++;
        final int num = (isRoot ? -1 : getCursor().getCurrentSiblingIndex());
        final Tree<T> node = new Tree<T>(vertex, i, num, getCursor().getDepth(), values);
        this.maxDegree = Math.max(maxDegree, getCursor().getAdapter().getDegreeOf(vertex));
        list.add(node);
        for (Tree<T> n : values) n.setParent(node);
        return node;
    }
}
