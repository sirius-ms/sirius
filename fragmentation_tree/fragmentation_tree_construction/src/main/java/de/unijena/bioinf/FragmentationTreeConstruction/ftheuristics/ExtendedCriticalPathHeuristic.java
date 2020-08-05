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
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.*;

public class ExtendedCriticalPathHeuristic {

    protected final boolean STOP_EARLY;
    protected final int INSERTION;

    protected FGraph graph;

    protected BitSet usedColors;
    protected Loss[] usedEdges;
    protected int numberOfSelectedEdges;
    protected ArrayList<Loss> selectableEdges;
    protected double[] criticalPaths;

    public ExtendedCriticalPathHeuristic(FGraph graph) {
        this(graph,true,1);
    }

    public ExtendedCriticalPathHeuristic(FGraph graph, boolean stopEarly, int insertion) {
        this.STOP_EARLY = stopEarly;
        this.INSERTION = insertion;
        this.graph = graph;
        this.usedColors = new BitSet(graph.maxColor()+1);
        this.usedEdges = new Loss[graph.maxColor()+1];
        this.numberOfSelectedEdges = 0;
        this.selectableEdges = new ArrayList(graph.maxColor()+1);
        this.criticalPaths = new double[graph.numberOfVertices()];
        if (graph.getRoot().getOutDegree()==1) {
            // just add this edge
            usedEdges[numberOfSelectedEdges++] = graph.getRoot().getOutgoingEdge(0);
            //System.out.println("ADD \"\" WITH WEIGHT " + graph.getRoot().getOutgoingEdge(0).getWeight() );
            addSeletableEdgesFor(graph.getRoot().getChildren(0));
        } else {
            addSeletableEdgesFor(graph.getRoot());
        }
        Arrays.fill(criticalPaths, Double.NaN);
    }

    protected void invalidateColor(int color) {
        final Fragment pseudoFragment = new Fragment(0,null,null);
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
    }

    public FTree solve() {
        while (findCriticalPaths()) {

        }
        if (INSERTION==1) relocateAll();
        else if (INSERTION==2) relocateBySpanningTree();
        return buildSolution();
    }

    protected FTree buildSolution() {
        if (numberOfSelectedEdges==0) {
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
        Arrays.sort(usedEdges, 0, numberOfSelectedEdges, Comparator.comparingInt(a -> a.getTarget().getColor()));
        final FTree tree = new FTree(usedEdges[0].getTarget().getFormula(), usedEdges[0].getTarget().getIonization());
        final HashMap<MolecularFormula, Fragment> fragmentsByFormula = new HashMap<>();
        fragmentsByFormula.put(tree.getRoot().getFormula(), tree.getRoot());
        for (int i=1; i < numberOfSelectedEdges; ++i) {
            final Fragment f = tree.addFragment(fragmentsByFormula.get(usedEdges[i].getSource().getFormula()), usedEdges[i].getTarget());
            f.getIncomingEdge().setWeight(usedEdges[i].getWeight());
            fragmentsByFormula.put(f.getFormula(), f);
        }
        double score = 0d;
        for (int i=0; i < numberOfSelectedEdges; ++i) {
            score += usedEdges[i].getWeight();
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
            final double criticalScore = recomputeCriticalScore(l.getTarget().getVertexId())+l.getWeight();
            if (criticalScore > bestPathScore) {
                bestPathScore = criticalScore;
                bestLoss = l;
            }
        }
        int maxColor = -1;
        if (bestLoss!=null)
            maxColor = backtrackBestPath(bestLoss,maxColor);
        selectableEdges.clear();
        for (int i=0, n=numberOfSelectedEdges; i < n; ++i) {
            addSeletableEdgesFor(usedEdges[i].getTarget());
        }

        if (maxColor >= 0) invalidateColor(maxColor);
        return bestLoss!=null;
    }

    protected int backtrackBestPath(Loss loss,int maxColor) {
        usedEdges[numberOfSelectedEdges++] = loss;
        //System.out.println("ADD " + loss + " WITH WEIGHT " + loss.getWeight() );
        assert usedColors.get(loss.getTarget().getColor())==false;
        usedColors.set(loss.getTarget().getColor());
        final Fragment u = loss.getTarget();
        maxColor = Math.max(u.getColor(), maxColor);
        double bestWeight = criticalPaths[u.getVertexId()];
        if (bestWeight+loss.getWeight()<=0 || STOP_EARLY) {
            return maxColor;
        }
        double heighestWeight = 0d;
        Loss bestLoss = null;
        for (int i=0, n = u.getOutDegree(); i < n; ++i) {
            final Loss uv = u.getOutgoingEdge(i);
            final double weight = criticalPaths[uv.getTarget().getVertexId()] + uv.getWeight();
            if (Double.isNaN(weight)) continue;
            if (weight>=bestWeight) {
                bestLoss = uv; break;
            } else if (weight >= heighestWeight)  {
                heighestWeight = weight; bestLoss = uv;
            }
        }
        if (bestLoss!=null)
            return backtrackBestPath(bestLoss, maxColor);
        return maxColor;
    }

    protected double recomputeCriticalScore(int vertexId) {
        if (!Double.isNaN(criticalPaths[vertexId]))
            return criticalPaths[vertexId];
        final Fragment u = graph.getFragmentAt(vertexId);
        criticalPaths[vertexId] = 0d;
        for (int i=0, n = u.getOutDegree(); i < n; ++i) {
            final Loss uv = u.getOutgoingEdge(i);
            if (!usedColors.get(uv.getTarget().getColor())) {
                final double weight = recomputeCriticalScore(uv.getTarget().getVertexId()) + uv.getWeight();
                criticalPaths[vertexId] = Math.max(criticalPaths[vertexId], weight);
            }
        }
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

    protected BitSet usedFragments;
    protected void relocateAll() {
        //double score=0d;
        this.usedFragments = new BitSet(graph.numberOfVertices());
        for (int l=0; l < numberOfSelectedEdges; ++l) {
            usedFragments.set(usedEdges[l].getTarget().getVertexId());
            //score += usedEdges[l].getWeight();
        }
        //int c = 0;
        for (int l=0; l < numberOfSelectedEdges; ++l) {
            //if (relocate(l)) ++c;
            relocate(l);
        }
    }

    protected void relocateBySpanningTree() {
        final TIntObjectHashMap<ArrayList<Loss>> availableLosses = new TIntObjectHashMap<>();
        final BitSet usedFragments = new BitSet(graph.numberOfVertices());
        for (int l=0; l < numberOfSelectedEdges; ++l) {
            final Loss loss = usedEdges[l];
            final Fragment f = loss.getTarget();
            usedFragments.set(f.getVertexId());
        }
        usedFragments.set(graph.getRoot().getVertexId());
        for (int l=0; l < numberOfSelectedEdges; ++l) {
            final Loss loss = usedEdges[l];
            final Fragment f = loss.getTarget();
            final ArrayList<Loss> ls = new ArrayList<>();
            for (int i=0; i < f.getInDegree(); ++i) {
                if (usedFragments.get(f.getParent(i).getVertexId())) {
                    ls.add(f.getIncomingEdge(i));
                }
            }
            availableLosses.put(f.getVertexId(), ls);
        }
        numberOfSelectedEdges = 0;
        while (!availableLosses.isEmpty()) {
            Loss maximum = findMax(availableLosses);
            usedEdges[numberOfSelectedEdges++] = maximum;
            availableLosses.remove(maximum.getTarget().getVertexId());
        }
    }

    protected Loss findMax(TIntObjectHashMap<ArrayList<Loss>> map) {
        Loss[] maxLoss = new Loss[1];
        map.forEachValue((x)->{
            for (Loss l : x) {
                if (maxLoss[0]==null || l.getWeight()>maxLoss[0].getWeight())
                    maxLoss[0] = l;
            }
            return true;
        });
        return maxLoss[0];
    }

    protected boolean relocate(int lossId) {
        Loss uv = usedEdges[lossId];
        final Fragment v = uv.getTarget();
        for (int l=0; l < v.getInDegree(); ++l) {
            final Loss x = v.getIncomingEdge(l);
            if (x.getWeight() > uv.getWeight() && usedFragments.get(x.getSource().getVertexId())) {
                uv = x;
            }
        }
        if (usedEdges[lossId]!=uv) {
            usedEdges[lossId] = uv;
            return true;
        } else return false;
    }
}
