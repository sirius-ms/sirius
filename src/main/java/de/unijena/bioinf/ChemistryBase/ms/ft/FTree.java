package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.graphUtils.tree.BackrefTreeAdapter;
import de.unijena.bioinf.graphUtils.tree.PostOrderTraversal;
import de.unijena.bioinf.graphUtils.tree.PreOrderTraversal;
import de.unijena.bioinf.graphUtils.tree.TreeCursor;

import java.util.*;

public class FTree extends AbstractFragmentationGraph {

    protected Fragment root;

    public FTree(MolecularFormula rootFormula) {
        this.root = addFragment(rootFormula);
    }

    public static BackrefTreeAdapter<Fragment> treeAdapter() {
        return new BackrefTreeAdapter<Fragment>() {
            @Override
            public Fragment getParent(Fragment node) {
                return node.getParent();
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

    public Fragment addFragment(Fragment parent, MolecularFormula child) {
        final MolecularFormula loss = parent.formula.subtract(child);
        if (!loss.isAllPositiveOrZero()) {
            throw new IllegalArgumentException(child.toString() + " cannot be child formula of " + parent.formula.toString());
        }
        final Fragment f = addFragment(child);
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
        final List<Fragment> children = new ArrayList<Fragment>(vertex.getChildren());
        final Fragment parent = vertex.getParent();
        deleteFragment(vertex);
        for (Fragment c : children) addLoss(parent, c);
        return children.size();
    }

    @Override
    public Iterator<Fragment> postOrderIterator(Fragment startingRoot) {
        return new PostOrderTraversal.TreeIterator<Fragment>(TreeCursor.getCursor(startingRoot, FTree.treeAdapter()));
    }

    @Override
    public Iterator<Fragment> preOrderIterator(Fragment startingRoot) {
        return new PreOrderTraversal.TreeIterator<Fragment>(TreeCursor.getCursor(startingRoot, FTree.treeAdapter()));
    }

    @Override
    public Fragment getRoot() {
        return root;
    }

    @Override
    public Iterator<Loss> lossIterator() {
        return new Iterator<Loss>() {
            final Iterator<Fragment> fiter = fragments.iterator();

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
                return getFragmentAt(index).getIncomingEdge();
            }

            @Override
            public int size() {
                return fragments.size();
            }
        };
    }

    @Override
    public int numberOfEdges() {
        return fragments.size();
    }

    @Override
    public Loss getLoss(Fragment u, Fragment v) {
        final Loss l = v.getIncomingEdge();
        if (l.source == u) return l;
        else return null;
    }

    public final TreeCursor getCursor() {
        return TreeCursor.getCursor(root, FTree.treeAdapter());
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
}
