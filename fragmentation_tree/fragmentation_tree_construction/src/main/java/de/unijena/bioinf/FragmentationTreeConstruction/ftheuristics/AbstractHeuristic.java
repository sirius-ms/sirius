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

package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics;

import de.unijena.bioinf.ChemistryBase.ms.ft.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractHeuristic {

    protected final FGraph graph;
    protected final List<Loss> selectedEdges;
    protected final int ncolors;

    protected final IntergraphMapping.Builder mapping;

    public AbstractHeuristic(FGraph graph) {
        this.ncolors = graph.maxColor()+1;
        this.graph = graph;
        this.selectedEdges = new ArrayList<>(ncolors);
        this.mapping = IntergraphMapping.build();
    }

    public abstract FTree solve();

    public IntergraphMapping.Builder getGraphMappingBuilder() {
        return mapping;
    }

    protected FTree buildSolution(boolean prune) {
        if (selectedEdges.size()==0) {
            Fragment bestFrag = null;
            for (Fragment f : graph.getRoot().getChildren()) {
                if (bestFrag==null || bestFrag.getIncomingEdge().getWeight() < f.getIncomingEdge().getWeight() ) {
                    bestFrag = f;
                }
            }
            final FTree t = new FTree(bestFrag.getFormula(), bestFrag.getIonization());
            t.setTreeWeight(bestFrag.getIncomingEdge().getWeight());
            mapping.mapLeftToRight(bestFrag, t.getRoot());
            return t;
        }
        selectedEdges.sort(Comparator.comparingInt((l)->l.getSource().getColor()));
        final FTree tree = new FTree(selectedEdges.get(0).getTarget().getFormula(), selectedEdges.get(0).getTarget().getIonization());
        mapping.mapLeftToRight(selectedEdges.get(0).getTarget(), tree.getRoot());
        //final HashMap<MolecularFormula, Fragment> fragmentsByFormula = new HashMap<>(selectedEdges.size());
        for (int i=1; i < selectedEdges.size(); ++i) {
            final Loss l = selectedEdges.get(i);
            Fragment parent = mapping.getMapping().get(l.getSource());
            final Fragment f = tree.addFragment(parent, l.getTarget());
            mapping.mapLeftToRight(l.getTarget(), f);
            f.getIncomingEdge().setWeight(l.getWeight());
        }
        if (prune) prune(tree, tree.getRoot());
        double score = selectedEdges.get(0).getWeight();
        for (Fragment f : tree) {
            if (!f.isRoot())
                score += f.getIncomingEdge().getWeight();
        }
        tree.setTreeWeight(score);
        return tree;
    }

    protected final double prune(FTree tree, Fragment f) {
        double score = 0d;
        if (!f.isRoot()) score += f.getIncomingEdge().getWeight();
        final Fragment[] children = new Fragment[f.getOutDegree()];
        for (int i=0; i < f.getOutDegree(); ++i) {
            children[i] = f.getChildren(i);
        }
        for (Fragment g : children) {
            score += Math.max(0, prune(tree, g));
        }
        if (score <= 0) {
            tree.deleteSubtree(f);
        }
        return score;
    }
}
