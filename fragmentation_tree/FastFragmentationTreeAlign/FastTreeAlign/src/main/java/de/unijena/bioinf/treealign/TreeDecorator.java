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
package de.unijena.bioinf.treealign;

import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;

import java.util.ArrayList;
import java.util.List;

public final class TreeDecorator<T> extends PostOrderTraversal.Call<T, Tree<T>> {
    private final ArrayList<Tree<T>> list;
    private final ArrayList<Tree<T>> leafs;
    public int maxDegree;
    private int index;

    public TreeDecorator(ArrayList<Tree<T>> list, ArrayList<Tree<T>> leafs) {
        this.index = 0;
        this.leafs = leafs;
        this.list = list;
    }

    public Tree<T> call(T vertex, List<Tree<T>> values, boolean isRoot) {
        assert isRoot == (getCursor().getDepth() == 0);
        final int i = (values.isEmpty() ? -1 : index++);
        final int num = (isRoot ? -1 : getCursor().getCurrentSiblingIndex());
        final Tree<T> node = new Tree<T>(vertex, i, num, getCursor().getDepth(), values);
        this.maxDegree = Math.max(maxDegree, getCursor().getAdapter().getDegreeOf(vertex));
        if (node.degree() > 0) {
            list.add(node);
        } else {
            leafs.add(node);
        }
        for (Tree<T> n : values) n.setParent(node);
        return node;
    }
}
