

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

import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;
import de.unijena.bioinf.graphUtils.tree.Tree;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.List;

public class FingerprintTree implements Cloneable {

    protected Tree<FPVariable> root;
    protected TIntObjectHashMap<FPVariable> varMap;
    protected ArrayList<Tree<FPVariable>> nodes;

    protected double[] frequencies;

    public FingerprintTree(Tree<FPVariable> root) {
        this.root = root;
        this.varMap = new TIntObjectHashMap<>();
        this.nodes = new ArrayList<>();
        for (Tree<FPVariable> node : PostOrderTraversal.create(root)) {
            varMap.put(node.getLabel().to, node.getLabel());
            nodes.add(node);
        }
    }

    public Tree<FPVariable> getRoot() {
        return root;
    }

    @Override
    public FingerprintTree clone() {
        return new FingerprintTree(PostOrderTraversal.create(root).call(new PostOrderTraversal.Call<Tree<FPVariable>, Tree<FPVariable>>() {
            @Override
            public Tree<FPVariable> call(Tree<FPVariable> vertex, List<Tree<FPVariable>> values, boolean isRoot) {
                final FPVariable cloned = vertex.getLabel().clone();
                final Tree<FPVariable> node = new Tree<FPVariable>(cloned);
                for (Tree<FPVariable> child : values) node.addChild(child);
                return node;
            }
        }));
    }
}
