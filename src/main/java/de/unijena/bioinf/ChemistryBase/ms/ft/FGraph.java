package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.*;

public class FGraph extends AbstractFragmentationGraph {
    private Fragment pseudoRoot;

    public FGraph() {
        super();
        this.pseudoRoot = addFragment(MolecularFormula.emptyFormula());
    }

    public FGraph(FGraph copy) {
        super(copy);
        this.pseudoRoot = fragments.get(0);
        assert pseudoRoot.isRoot();
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

    public Fragment addFragment(MolecularFormula formula) {
        return super.addFragment(formula);
    }

    public void deleteFragment(Fragment f) {
        super.deleteFragment(f);
    }

    public Fragment addRootVertex(MolecularFormula formula) {
        final Fragment f = addFragment(formula);
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
            for (Loss l : u.outgoingEdges) {
                if (l.source == u && l.target == v) {
                    return l;
                }
            }
        } else {
            for (Loss l : v.incomingEdges) {
                if (l.source == u && l.target == v) {
                    return l;
                }
            }
        }
        return null;
    }

    public boolean disconnect(Fragment u, Fragment v) {
        if (u.outDegree < v.inDegree) {
            for (Loss l : u.outgoingEdges) {
                if (l.source == u && l.target == v) {
                    deleteLoss(l);
                    return true;
                }
            }
        } else {
            for (Loss l : v.incomingEdges) {
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

        private void goToFirstLeaf() {
            current = root;
            while (!current.isLeaf()) {
                Loss l = current.getOutgoingEdge(0);
                stack.add(l);
                current = l.target;
            }
        }

        void fetchNext() {
            if (stack.isEmpty()) {
                current = root;
                state = 3;
            } else {
                final Loss l = stack.remove(stack.size() - 1);
                int nextLossId;
                for (nextLossId = l.sourceEdgeOffset + 1; nextLossId < l.sourceEdgeOffset; ++nextLossId) {
                    final Fragment f = l.source.outgoingEdges[nextLossId].target;
                    if ((allowedColors == null || allowedColors.get(f.color)) && (!visitedNodes.get(f.vertexId))) break;
                }
                if (nextLossId >= l.source.outDegree) {
                    current = l.source;
                } else {
                    final Loss newLoss = l.source.getOutgoingEdge(nextLossId);
                    stack.add(newLoss);
                    current = newLoss.target;
                }
            }
            visitedNodes.set(current.vertexId);
        }

        @Override
        public boolean hasNext() {
            return state < 3;
        }

        @Override
        public Fragment next() {
            switch (state) {
                case 0:
                    goToFirstLeaf();
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
