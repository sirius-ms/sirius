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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.*;

public class CriticalPathInsertionHeuristic2 extends AbstractHeuristic {

    protected FGraph graph;

    protected BitSet usedColors;
    protected ArrayList<Loss> selectableEdges;
    protected double[] criticalPaths;
    private final TIntObjectHashMap<Loss> color2Edge;
    protected final TIntArrayList usedColorList;

    protected final double[] maxOut;
    protected final double[] maxCriticalScore;
    protected final Loss[] maxOutLoss;

    public CriticalPathInsertionHeuristic2(FGraph graph) {
        super(graph);
        this.graph = graph;
        this.usedColors = new BitSet(ncolors+1);
        this.selectableEdges = new ArrayList<Loss>(ncolors+1);
        this.criticalPaths = new double[graph.numberOfVertices()];
        color2Edge = new TIntObjectHashMap<>(ncolors, 0.75f, -1);
        Arrays.fill(criticalPaths, Double.NaN);
        this.maxOut = new double[graph.numberOfVertices()];
        this.maxOutLoss = new Loss[graph.numberOfVertices()];
        this.maxCriticalScore = new double[graph.numberOfVertices()];
        usedColorList = new TIntArrayList(ncolors);
    }


    private void insert(Loss maxLoss) {
        final Fragment newVertex = maxLoss.getTarget();
        usedColors.set(newVertex.getColor());
        usedColorList.add(newVertex.getColor());
        color2Edge.put(newVertex.getColor(), maxLoss);
        // relocate and update
        for (int i = 0, n = newVertex.getOutDegree(); i < n; ++i) {
            final Loss l = newVertex.getOutgoingEdge(i);
            final Fragment w = l.getTarget();
            final Loss xw = color2Edge.get(w.getColor());
            if (xw != null) {
                if (xw.getTarget() == w && xw.getWeight() < l.getWeight()) {
                    color2Edge.put(w.getColor(), l);
                    for (int j = 0, m = w.getInDegree(); j < m; ++j) {
                        final Loss zw = w.getIncomingEdge(j);
                        final Fragment z = zw.getSource();
                        final int zid = z.getVertexId();
                        if (maxOut[zid] > Double.NEGATIVE_INFINITY) {
                            if (usedColors.get(z.getColor())) {
                                maxOut[zid]=Double.NEGATIVE_INFINITY;
                            } else {
                                maxOut[zid] = Math.max(
                                        0,
                                        maxOut[zid] + xw.getWeight() - l.getWeight()
                                );
                            }
                        }
                    }
                }
            }
        }
        for (int i=0, n = newVertex.getInDegree(); i < n; ++i) {
            final Loss yv = newVertex.getIncomingEdge(i);
            if (yv.getWeight() > maxLoss.getWeight()) {
                int y = yv.getSource().getVertexId();
                maxOut[y] += yv.getWeight()-maxLoss.getWeight();
            }
        }

    }

    private void initialize() {
        final Loss rootLoss = graph.getRoot().getOutgoingEdge(0);
        final Fragment root = rootLoss.getTarget();
        maxOut[root.getVertexId()] = Double.NEGATIVE_INFINITY;
        usedColors.set(rootLoss.getTarget().getColor());
        usedColorList.add(rootLoss.getTarget().getColor());
        color2Edge.put(rootLoss.getTarget().getColor(), rootLoss);
        if (graph.getRoot().getOutDegree()==1) {
            addSeletableEdgesFor(root);
        } else {
            throw new RuntimeException("Algorithm is optimized for graphs with one tree root");
        }

    }

    protected void invalidateColor(int color) {
        /*
        final Fragment pseudoFragment = new Fragment(0,null);
        pseudoFragment.setColor(color);
        int searchKey = Collections.binarySearch(graph.getFragments(), pseudoFragment,new Comparator<Fragment>() {
            @Override
            public int compare(Fragment o1, Fragment o2) {
                return o1.getColor()-o2.getColor();
            }
        });
        if (searchKey < 0) {
            searchKey = -(searchKey+1);
        } else {
            while (searchKey< graph.numberOfVertices() && graph.getFragmentAt(searchKey).getColor() == color)
                ++searchKey;
        }
        Arrays.fill(criticalPaths, 0, searchKey, Double.NaN);
        */
        Arrays.fill(criticalPaths, 0, criticalPaths.length, Double.NaN);
    }

    public FTree solve() {
        initialize();
        while (findCriticalPaths()) {

        }
        return buildSolution();
    }

    protected FTree buildSolution() {
        if (usedColorList.size()<=0) {
            Fragment bestFrag = null;
            for (Fragment f : graph.getRoot().getChildren()) {
                if (bestFrag==null || bestFrag.getIncomingEdge().getWeight() < f.getIncomingEdge().getWeight() ) {
                    bestFrag = f;
                }
            }
            final FTree t = new FTree(bestFrag.getFormula(), bestFrag.getIonization());
            t.setTreeWeight(bestFrag.getIncomingEdge().getWeight());
            return t;
        }
        selectedEdges.addAll(color2Edge.valueCollection());
        selectedEdges.sort(Comparator.comparingInt(a -> a.getTarget().getColor()));
        final FTree tree = new FTree(selectedEdges.get(0).getTarget().getFormula(), selectedEdges.get(0).getTarget().getIonization());
        final HashMap<MolecularFormula, Fragment> fragmentsByFormula = new HashMap<>();
        fragmentsByFormula.put(tree.getRoot().getFormula(), tree.getRoot());
        double score = selectedEdges.get(0).getWeight();
        for (int i=1; i < selectedEdges.size(); ++i) {
            final Loss L = selectedEdges.get(i);
            final Fragment f = tree.addFragment(fragmentsByFormula.get(L.getSource().getFormula()), L.getTarget());
            f.getIncomingEdge().setWeight(L.getWeight());
            fragmentsByFormula.put(f.getFormula(), f);
            score += L.getWeight();
        }
        tree.setTreeWeight(score);
        return tree;
    }

    /*
     SIMPLE CASE: Graph is layered (i.e. no isotope peaks!)
     */
    protected boolean findCriticalPaths() {
        //System.out.println(".....");
        //Arrays.fill(criticalPaths, Double.NaN);
        double bestPathScore = 0d;
        Loss bestLoss = null;
        for (Loss l : selectableEdges) {
            final double criticalScore = recomputeCriticalScore(l.getTarget().getVertexId())+l.getWeight() + maxCriticalScore[l.getTarget().getVertexId()];
            if (criticalScore > bestPathScore) {
                bestPathScore = criticalScore;
                bestLoss = l;
            }
        }
        if (bestLoss==null) return false;
        final Fragment u = bestLoss.getTarget();
        invalidateColor(u.getColor());
        insert(bestLoss);
        selectableEdges.clear();
        for (int i=0, n=usedColorList.size(); i < n; ++i) {
            addSeletableEdgesFor(color2Edge.get(usedColorList.getQuick(i)).getTarget());
        }

        return true;
    }

    protected double recomputeCriticalScore(int vertexId) {
        if (!Double.isNaN(criticalPaths[vertexId]))
            return criticalPaths[vertexId];
        final Fragment u = graph.getFragmentAt(vertexId);
        criticalPaths[vertexId] = 0d;
        double crit = maxOut[vertexId];
        for (int i=0, n = u.getOutDegree(); i < n; ++i) {
            final Loss uv = u.getOutgoingEdge(i);
            if (!usedColors.get(uv.getTarget().getColor())) {
                final int fid = uv.getTarget().getVertexId();
                final double weight = recomputeCriticalScore(fid) + uv.getWeight();
                criticalPaths[vertexId] = Math.max(criticalPaths[vertexId], weight);
                crit = Math.max(maxCriticalScore[fid], crit);
            }
        }
        maxCriticalScore[vertexId] = crit;
        return criticalPaths[vertexId];
    }


    protected void addSeletableEdgesFor(Fragment root) {
        for (int i=0, n = root.getOutDegree(); i < n; ++i) {
            final Loss l = root.getOutgoingEdge(i);
            if (!usedColors.get(l.getTarget().getColor())) {
                selectableEdges.add(l);
            }
        }
    }
}
