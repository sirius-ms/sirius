
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

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.graphUtils.tree.*;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;

import java.util.*;

public class FTree extends AbstractFragmentationGraph implements ResultAnnotation {

    protected Fragment root;

    /**
     * The total score of the tree. This is the sum of scores of all losses plus the root score
     */
    protected double treeWeight;

    /**
     * Due to a very bad design decision, we cannot assign scores to fragments. Thus, we have to store
     * the score of the root separately.
     */
    protected double rootScore;

    public FTree(MolecularFormula rootFormula, Ionization ionization) {
        this.root = addFragment(rootFormula, ionization);
        this.rootScore = 0d;
    }

    /**
     * Creates a new FTree, using information from rootFragment but not the object itself
     * @param rootFragment
     */
    public FTree(Fragment rootFragment) {
        this.root = addFragment(rootFragment.getFormula(), rootFragment.getIonization());
    }

    public FTree(FTree copy) {
        super(copy);
        this.root = fragments.get(0);
        this.rootScore = copy.rootScore;
        this.treeWeight = copy.treeWeight;
        assert root.isRoot();
    }

    public static Comparator<FTree> orderByScoreDescending() {
        return new Comparator<FTree>() {
            @Override
            public int compare(FTree o1, FTree o2) {
                return Double.compare(o2.treeWeight, o1.treeWeight);
            }
        };
    }



    /**
     * Add a new root to the tree and connecting it with the previous root
     * @param newRoot
     */
    public Fragment addRoot(MolecularFormula newRoot, Ionization newRootIon) {
        final MolecularFormula loss = newRoot.subtract(root.getFormula());
        if (!loss.isAllPositiveOrZero()) {
            throw new IllegalArgumentException(root.getFormula().toString() + " cannot be child formula of " + newRoot.toString());
        }
        final Fragment f = addFragment(newRoot, newRootIon);
        addLoss(f, root);
        fragments.set(0, f);
        fragments.set(f.vertexId, root);
        root.setVertexId(f.vertexId);
        f.setVertexId(0);
        root = f;
        return f;
    }

    public double getRootScore() {
        return rootScore;
    }

    public void setRootScore(double score) {
        this.rootScore = score;
    }

    public double getTreeWeight() {
        return treeWeight;
    }

    public void setTreeWeight(double weight) {
        this.treeWeight = weight;
    }

    /*
    public void swapRoot(Fragment f) {
        if (!isOwnFragment(f)) throw new IllegalArgumentException("Expect a fragment of the same tree as parameter");
        fragments.set(0, f);
        fragments.set(f.vertexId, root);
        root.setVertexId(f.vertexId);
        f.setVertexId(0);
        invertPathsForRootSwapping(f);
        root = f;
    }

    protected void invertPathsForRootSwapping(Fragment v) {
        // all incoming egdes become outgoing edges
        final ArrayList<Loss> incomingLosses = new ArrayList<Loss>(v.getIncomingEdges());
        for (Loss l : incomingLosses) {
            final Fragment u = l.getSource();
            // u becomes child of v
            invertPathsForRootSwapping(u);
            // delete edge u->v
            deleteLoss(getLoss(u, v));
            // add new edge v<-u
            swapLoss(u, v);
        }
    }
    */

    public TreeAdapter<Fragment> treeAdapter() {
        return treeAdapterStatic();
    }

    // todo proof if static is a problem
    public static TreeAdapter<Fragment> treeAdapterStatic() {
        return new BackrefTreeAdapter<Fragment>() {
            @Override
            public Fragment getParent(Fragment node) {
                return node.inDegree == 0 ? null : node.getParent();
            }

            @Override
            public int indexOfSibling(Fragment node) {
                return node.getIncomingEdge().sourceEdgeOffset;
            }

            @Override
            public int getDepth(Fragment node) {
                int d = 0;
                while (!node.isRoot()) {
                    ++d;
                    node = node.getParent();
                }
                return d;
            }

            @Override
            public int getDegreeOf(Fragment vertex) {
                return vertex.getOutDegree();
            }

            @Override
            public List<Fragment> getChildrenOf(Fragment vertex) {
                return vertex.getChildren();
            }
        };
    }

    /**
     * it is recommended to rather use addFragment(Fragment parent, Fragment child), if possible
     * @param parent
     * @param child
     * @param childIon
     * @return
     */
    public Fragment addFragment(Fragment parent, MolecularFormula child, Ionization childIon) {
        final MolecularFormula loss = parent.formula.subtract(child);
        if (!loss.isAllPositiveOrZero()) {
            throw new IllegalArgumentException(child.toString() + " cannot be child formula of " + parent.formula.toString());
        }
        final Fragment f = addFragment(child, childIon);
        addLoss(parent, f);
        return f;
    }

    /**
     * Adds a new fragment to the tree and using information from child Fragment but not the object itself
     * @param parent
     * @param child
     * @return
     */
    public Fragment addFragment(Fragment parent, Fragment child) {
        final MolecularFormula loss = parent.formula.subtract(child.getFormula());
        if (!loss.isAllPositiveOrZero()) {
            throw new IllegalArgumentException(child.toString() + " cannot be child formula of " + parent.formula.toString());
        }
        final Fragment f = addFragment(child.getFormula(), child.getIonization());
        addLoss(parent, f);
        return f;
    }

    /**
     * Next to adding new fragments and contracting edges this is the only allowed modification of fragmentation trees:
     * It removes the incoming edge of vertex and add a new edge from newParent to vertex
     *
     * @param vertex
     * @param newParent
     * @return the newly created edge
     */
    public Loss swapLoss(Fragment vertex, Fragment newParent) {
        super.deleteLoss(vertex.getIncomingEdge());
        return super.addLoss(newParent, vertex);
    }

    /**
     * Delete a vertex, connect all its children to its parent
     *
     * @param vertex
     * @return number of newly created edges
     */
    public int deleteVertex(Fragment vertex) {
        if (fragments.get(vertex.vertexId)!=vertex)
            throw new IllegalArgumentException("given vertex stems from a different graph");
        final List<Fragment> children = new ArrayList<Fragment>(vertex.getChildren());
        final Fragment parent = vertex.getParent();
        deleteFragment(vertex);
        for (Fragment c : children) addLoss(parent, c);
        return children.size();
    }

    public int deleteSubtree(Fragment root) {
        assert !root.isDeleted();
        if (fragments.get(root.vertexId)!=root)
            throw new IllegalArgumentException("given vertex stems from a different graph");
        if (root.isLeaf()) {
            deleteFragment(root);
            return 1;
        }
        final Iterator<Fragment> iter = postOrderIterator(root);
        final ArrayList<Fragment> vertices = new ArrayList<Fragment>();
        while (iter.hasNext()) {
            vertices.add(iter.next());
        }
        for (Fragment f : vertices) {
            deleteFragment(f);
        }
        return vertices.size();
    }

    @Override
    public final Iterator<Fragment> postOrderIterator(Fragment startingRoot) {
        return PostOrderTraversal.createSubtreeTraversal(startingRoot, treeAdapter()).iterator();
    }

    @Override
    public final Iterator<Fragment> preOrderIterator(Fragment startingRoot) {
        return new PreOrderTraversal.TreeIterator<Fragment>(TreeCursor.getCursor(startingRoot, treeAdapter()));
    }

    @Override
    public Fragment getRoot() {
        return root;
    }

    @Override
    public Iterator<Loss> lossIterator() {
        final Iterator<Fragment> fiter = fragments.iterator();
        fiter.next(); // ignore root
        return new Iterator<Loss>() {

            @Override
            public boolean hasNext() {
                return fiter.hasNext();
            }

            @Override
            public Loss next() {
                return fiter.next().getIncomingEdge();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public List<Loss> losses() {
        return new AbstractList<Loss>() {
            @Override
            public Loss get(int index) {
                return getFragmentAt(index + 1).getIncomingEdge();
            }

            @Override
            public int size() {
                return fragments.size() - 1;
            }
        };
    }

    @Override
    public int numberOfEdges() {
        return fragments.size()-1;
    }

    @Override
    public Loss getLoss(Fragment u, Fragment v) {
        final Loss l = v.getIncomingEdge();
        if (l.source == u) return l;
        else return null;
    }

    public final TreeCursor<Fragment> getCursor() {
        return TreeCursor.getCursor(root, treeAdapter());
    }

    public final TreeCursor getCursor(Fragment f) {
        if (fragments.get(f.vertexId) != f)
            throw new IllegalArgumentException("vertex " + f + " does not belong to this graph");
        return TreeCursor.getCursor(f, treeAdapter());
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
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    protected Loss addLoss(Fragment u, Fragment v, MolecularFormula f) {
        if (v.inDegree > 0 && v.getIncomingEdge().source != u)
            throw new RuntimeException("Fragment " + v + " already have a parent.");
        final Loss loss = new Loss(u, v, f, 0d);
        if (u.outgoingEdges.length <= u.outDegree) {
            u.outgoingEdges = Arrays.copyOf(u.outgoingEdges, u.outDegree + 1);
        }
        u.outgoingEdges[u.outDegree] = loss;
        loss.sourceEdgeOffset = u.outDegree++;
        if (v.incomingEdges.length <= v.inDegree) {
            v.incomingEdges = Arrays.copyOf(v.incomingEdges, v.inDegree + 1);
        }
        v.incomingEdges[v.inDegree] = loss;
        loss.targetEdgeOffset = v.inDegree++;
        ++edgeNum;
        return loss;
    }

    public void normalizeStructure() {
        for (Fragment f : fragments) {
            final List<Loss> childs = new ArrayList<Loss>(f.getOutgoingEdges());
            Collections.sort(childs, new Comparator<Loss>() {
                @Override
                public int compare(Loss loss, Loss loss2) {
                    return loss.getTarget().getFormula().compareTo(loss2.getTarget().getFormula());
                }
            });
            int i = 0;
            for (Loss l : childs) {
                f.outgoingEdges[i] = l;
                l.sourceEdgeOffset = i;
                ++i;
            }
        }
        Collections.sort(fragments, new Comparator<Fragment>() {
            @Override
            public int compare(Fragment o1, Fragment o2) {
                return Double.compare(o2.getFormula().getMass(), o1.getFormula().getMass());
            }
        });
        for (int k=0; k < fragments.size(); ++k) {
            fragments.get(k).setVertexId(k);
        }

    }


}
