
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

package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import gnu.trove.list.array.TIntArrayList;

import java.util.*;

public class FGraph extends AbstractFragmentationGraph {
    private Fragment pseudoRoot;

    public FGraph() {
        super();
        this.pseudoRoot = addFragment(MolecularFormula.emptyFormula(), new Charge(0));
        this.pseudoRoot.setColor(-1); //pseudo root must not correspond to any peak.
    }

    public FGraph(FGraph copy) {
        super(copy);
        this.pseudoRoot = fragments.get(0);
        assert pseudoRoot.isRoot();
        assert pseudoRoot.getColor()==-1;
    }

    @Override
    public Iterator<Fragment> iterator() {
        return new Iterator<Fragment>() {
            int k = 0;

            @Override
            public boolean hasNext() {
                return k < fragments.size();
            }

            @Override
            public Fragment next() {
                if (!hasNext()) throw new NoSuchElementException();
                return fragments.get(k++);
            }

            @Override
            public void remove() {
                if (k == 0) throw new IllegalStateException();
                deleteFragment(fragments.get(k - 1));
            }
        };
    }

    // - for non-isotopes we have: xi,xi+1 -> mz(xi) > mz(xi+1)
    // - for isotope peaks we have:


    public void reorderVertices(List<Fragment> newOrder) {
        this.fragments.clear();
        this.fragments.addAll(newOrder);
        int id=0;
        for (Fragment f : fragments) {
            f.setVertexId(id++);
        }
    }

    @Override
    public Fragment getRoot() {
        return pseudoRoot;
    }


    public List<List<Fragment>> verticesPerColor() {
        final ArrayList<List<Fragment>> verticesPerColor = new ArrayList<List<Fragment>>();
        for (Fragment f : fragments) {
            final int color = f.getColor();
            if (color >= verticesPerColor.size()) {
                verticesPerColor.ensureCapacity(color + 1);
                for (int k = verticesPerColor.size(); k <= color; ++k)
                    verticesPerColor.add(new ArrayList<Fragment>());
            }
            verticesPerColor.get(color).add(f);
        }
        return verticesPerColor;
    }

    @Override
    public Iterator<Loss> lossIterator() {
        return new LossIterator();
    }

    @Override
    public List<Loss> losses() {
        final ArrayList<Loss> losses = new ArrayList<Loss>(numberOfEdges());
        for (Fragment f : fragments) {
            for (int i = 0; i < f.inDegree; ++i) {
                losses.add(f.getIncomingEdge(i));
            }
        }
        return losses;
    }

    @Override
    public int numberOfEdges() {
        return edgeNum;
    }

    public int maxColor() {
        int maxColor = 0;
        for (Fragment f : fragments) maxColor = Math.max(f.getColor(), maxColor);
        return maxColor;
    }

    public Fragment addFragment(MolecularFormula formula, Ionization ionization) {
        return super.addFragment(formula, ionization);
    }

    public void deleteFragment(Fragment f) {
        super.deleteFragment(f);
    }

    public void deleteFragmentsKeepTopologicalOrder(Iterable<Fragment> todelete) {
        super.deleteFragmentsKeepTopologicalOrder(todelete, null,null);
    }

    public void deleteFragmentsKeepTopologicalOrder(Iterable<Fragment> todelete, TIntArrayList idsFrom, TIntArrayList idsTo) {
        super.deleteFragmentsKeepTopologicalOrder(todelete, idsFrom, idsTo);
    }

    public boolean isConnectedGraph() {
        final boolean[] connectedVertices = new boolean[numberOfVertices()];
        connectedVertices[getRoot().getVertexId()] = true;
        for (int i=0; i < numberOfVertices(); ++i) {
            Fragment u = getFragmentAt(i);
            while (!connectedVertices[u.getVertexId()]) {
                if (u.inDegree==0) return false;
                u = u.getParent();
            }
            connectedVertices[i]=true;
        }
        for (final boolean val : connectedVertices) {
            if (!val) return false;
        }
        return true;
    }

    public boolean isValidNumbered() {
        for (int i=0; i < numberOfVertices(); ++i) {
            final Fragment u = fragments.get(i);
            if (u.getVertexId()!=i) {
                assert false;
                return false;
            }
            for (int l=0; l < u.outDegree; ++l) {
                if (u.getOutgoingEdge(l).sourceEdgeOffset != l) {
                    assert false;
                    return false;
                }
            }
            for (int l=0; l < u.inDegree; ++l) {
                if (u.getIncomingEdge(l).targetEdgeOffset != l) {
                    assert false;
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isTopologicalOrdered()  {
        if (!getFragmentAt(0).isRoot()) return false;
        for (int k=2; k < numberOfVertices(); ++k) {
            final Fragment u = getFragmentAt(k-1);
            final Fragment v = getFragmentAt(k);
            if (u.color > v.color) return false;
            final int N = u.outDegree;
            for (int i=1; i < N; ++i) {
                final Loss a = u.getOutgoingEdge(i-1);
                final Loss b = u.getOutgoingEdge(i);
                if (a.getTarget().color > b.getTarget().color) return false;
            }
        }
        return true;

    }

    public Fragment addRootVertex(MolecularFormula formula, Ionization ionization) {
        final Fragment f = addFragment(formula, ionization);
        pseudoRoot.setFormula(pseudoRoot.getFormula(), ionization);
        addLoss(pseudoRoot, f, MolecularFormula.emptyFormula());
        return f;
    }

    public Loss addLoss(Fragment u, Fragment v) {
        return super.addLoss(u, v);
    }

    public void deleteLoss(Loss l) {
        super.deleteLoss(l);
    }

    @Override
    public Loss getLoss(Fragment u, Fragment v) {
        if (u.outDegree < v.inDegree) {
            for (int li =0; li < u.outDegree; ++li) {
                Loss l = u.outgoingEdges[li];
                if (l.source == u && l.target == v) {
                    return l;
                }
            }
        } else {
            for (int li =0; li < v.inDegree; ++li) {
                Loss l = v.incomingEdges[li];
                if (l.source == u && l.target == v) {
                    return l;
                }
            }
        }
        return null;
    }

    public boolean disconnect(Fragment u, Fragment v) {
        if (u.outDegree < v.inDegree) {
            for (int li =0; li < u.outDegree; ++li) {
                Loss l = u.outgoingEdges[li];
                if (l.source == u && l.target == v) {
                    deleteLoss(l);
                    return true;
                }
            }
        } else {
            for (int li =0; li < v.inDegree; ++li) {
                Loss l = v.incomingEdges[li];
                if (l.source == u && l.target == v) {
                    deleteLoss(l);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Iterator<Fragment> postOrderIterator(Fragment startingRoot) {
        return new PostOrderIterator(this, startingRoot);
    }

    @Override
    public Iterator<Fragment> preOrderIterator(Fragment startingRoot) {
        return new PreOrderIterator(this, startingRoot);
    }

    public Iterator<Fragment> postOrderIterator(Fragment startingRoot, BitSet allowedColors) {
        return new PostOrderIterator(this, startingRoot, (BitSet) allowedColors.clone());
    }

    public Iterator<Fragment> preOrderIterator(Fragment startingRoot, BitSet allowedColors) {
        return new PreOrderIterator(this, startingRoot, (BitSet) allowedColors.clone());
    }

    /**
     * Fast method to delete outgoing losses of fragment u. Be careful! Losses to delete
     * have to be in topological order! This method guarantees to keep the topological
     * order for outgoing edges in fragment u
     * @param u
     * @param toDelete
     */
    public void deleteLossesKeepTopologialOrder(Fragment u, ArrayList<Loss> toDelete) {
        final int N = u.getOutDegree();
        int l = 0;
        int i = toDelete.get(0).sourceEdgeOffset;
        int j = i;
        while(j < N) {
            while (l < toDelete.size() && j==toDelete.get(l).sourceEdgeOffset) {
                ++j;
                ++l;
            }
            if (j < N) {
                u.outgoingEdges[i] = u.outgoingEdges[j];
                u.outgoingEdges[i].sourceEdgeOffset = i;
            } else {
                u.outgoingEdges[i] = null;
            }
            ++i; ++j;
        }
        if (i < N) Arrays.fill(u.outgoingEdges, i, N, null);
        u.outDegree -= toDelete.size();
        // now free losses of childs
        // this time we do not have to care about topological ordering
        for (Loss loss : toDelete) {
            final Fragment child = loss.getTarget();
            if (loss.targetEdgeOffset+1 < child.inDegree) {
                final Loss lastLoss = child.incomingEdges[child.inDegree-1];
                child.incomingEdges[loss.targetEdgeOffset] = lastLoss;
                lastLoss.targetEdgeOffset = loss.targetEdgeOffset;
            }
            assert child.incomingEdges[child.inDegree-1] != null;
            child.incomingEdges[--child.inDegree] = null;
        }
    }

    private final static class PostOrderIterator implements Iterator<Fragment> {
        private final FGraph graph;
        private final ArrayList<Loss> stack;
        private final Fragment root;
        private final BitSet allowedColors;
        private final BitSet visitedNodes;
        private Fragment current;
        private byte state; // 0=initial, 1=normal, 2=alreadyFetched, 3=AtRoot

        PostOrderIterator(FGraph graph, Fragment root, BitSet allowedColors) {
            this.root = root;
            this.allowedColors = allowedColors;
            this.stack = new ArrayList<Loss>();
            this.current = null;
            this.graph = graph;
            this.visitedNodes = new BitSet(graph.numberOfVertices());
            this.state = 0;
        }

        PostOrderIterator(FGraph graph, Fragment root) {
            this(graph, root, null);
        }

        /*
        go to lowest unvisited descendant beginning in the given vertex
         */
        private void goToFirstLeaf(Fragment startVertex) {
            current = startVertex;
            visitedNodes.set(startVertex.getVertexId());
            eachPathNode:
            while (!current.isLeaf()) {
                Loss l = null;
                boolean foundOne = false;
                for (int i = 0; i < current.outDegree; ++i) {
                    l = current.getOutgoingEdge(i);
                    if (!visitedNodes.get(l.getTarget().getVertexId()) && (allowedColors == null || allowedColors.get(l.getTarget().getColor()))) {
                        foundOne = true;
                        break;
                    }
                }
                if (!foundOne) {
                    // it seems that you are the last descendant
                    break eachPathNode;
                }
                stack.add(l);
                visitedNodes.set(l.target.getVertexId());
                current = l.target;
            }
        }

        void fetchNext() {
            if (!stack.isEmpty()) {
                final Loss l = stack.get(stack.size() - 1);
                // search for sibling
                int nextLossId;
                boolean found = false;
                for (nextLossId = l.sourceEdgeOffset + 1; nextLossId < l.getSource().outDegree; ++nextLossId) {
                    final Fragment f = l.source.outgoingEdges[nextLossId].target;
                    if (!visitedNodes.get(f.vertexId) && (allowedColors == null || allowedColors.get(f.color))) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    goToFirstLeaf(l.source.getChildren(nextLossId));
                    return;
                } else {
                    stack.remove(stack.size() - 1);
                    current = l.getSource();
                    if (current == root) state = 3;
                    return;
                }
            } else {
                current = root;
                state = 3;
            }
        }

        @Override
        public boolean hasNext() {
            return state < 3;
        }

        @Override
        public Fragment next() {
            switch (state) {
                case 0:
                    goToFirstLeaf(root);
                    state = (byte) ((current == root) ? 3 : 1);
                    return current;
                case 1:
                    fetchNext();
                    return current;
                case 2:
                    state = (byte) ((current == root) ? 3 : 1);
                    return current;
                default:
                    throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            if (current == null || state == 2) throw new IllegalStateException();
            if (state == 3) {
                graph.deleteFragment(root);
            } else {
                final Loss parentLoss = stack.get(stack.size() - 1);
                final Fragment parent = parentLoss.source;
                final int offset = parentLoss.sourceEdgeOffset;
                graph.deleteFragment(current);
                if (parent.outDegree > offset) {
                    current = parent.getChildren(offset);
                    stack.add(parent.getOutgoingEdge(offset));
                } else {
                    current = parent;
                }
                state = 2;
            }
        }
    }

    private final static class PreOrderIterator implements Iterator<Fragment> {
        private final ArrayList<Fragment> stack;
        private final BitSet allowedColors;
        private final BitSet visitedNodes;

        PreOrderIterator(FGraph graph, Fragment root) {
            this(graph, root, null);
        }

        PreOrderIterator(FGraph graph, Fragment root, BitSet allowedColors) {
            this.allowedColors = allowedColors;
            this.stack = new ArrayList<Fragment>();
            this.visitedNodes = new BitSet(graph.numberOfVertices());
            stack.add(root);
        }


        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public Fragment next() {
            if (!hasNext()) throw new NoSuchElementException();
            final Fragment u = stack.remove(stack.size() - 1);
            for (int k = 0; k < u.outDegree; ++k) {
                final Fragment v = u.getChildren(k);
                if (!visitedNodes.get(v.vertexId) && (allowedColors == null || allowedColors.get(v.color))) {
                    stack.add(v);
                    visitedNodes.set(v.vertexId);
                }
            }
            return u;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final class LossIterator implements Iterator<Loss> {
        int nextLossNumber;
        private Loss nextLoss, lastLoss;
        private Fragment fragment;
        private Iterator<Fragment> fiter;

        private LossIterator() {
            this.nextLoss = null;
            this.lastLoss = null;
            this.fiter = fragments.iterator();
            this.fragment = fiter.next();
            this.nextLossNumber = 0;
            fetchNext();
        }


        @Override
        public boolean hasNext() {
            return nextLoss != null;
        }

        @Override
        public Loss next() {
            if (nextLoss == null) throw new NoSuchElementException();
            lastLoss = nextLoss;
            fetchNext();
            return lastLoss;
        }

        @Override
        public void remove() {
            if (lastLoss == null) throw new IllegalStateException();
            deleteLoss(lastLoss);
            --nextLossNumber;
        }

        private void fetchNext() {
            while (nextLossNumber >= fragment.inDegree) {
                if (fiter.hasNext()) {
                    fragment = fiter.next();
                    nextLossNumber = 0;
                } else {
                    nextLoss = null;
                    return;
                }
            }
            nextLoss = fragment.getIncomingEdge(nextLossNumber++);
        }
    }

}
