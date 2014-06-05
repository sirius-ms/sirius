package de.unijena.bioinf.FragmentationTreeConstruction.model.graph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class GraphTraversal {

    /**
     * Traverses first the node itself, then it's children from left to right
     */
    public static class InOrderIterator implements Iterator<FTVertex> {
        private ArrayList<FTVertex> stack;
        private BitSet visitedNodes;

        public InOrderIterator(FTGraph graph) {
            this(graph.getRoot());
        }

        public InOrderIterator(FTVertex root) {
            stack = new ArrayList<FTVertex>();
            stack.add(root);
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public FTVertex next() {
            final FTVertex v = stack.remove(stack.size()-1);
            for (FTEdge edge : v.outgoingEdges) {
                if (edge!=null && !visitedNodes.get(edge.to.index)) {
                    stack.add(edge.to);
                    visitedNodes.set(edge.to.index);
                }
            }
            return v;
        }

        public FTVertex peekNextVertex() {
            return stack.get(stack.size()-1);
        }

        public void skipVertexAndSubtree() {
            stack.remove(stack.size()-1);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Traverses first the children from left to right, then the node itself
     */
    public static class PostOrderIterator implements Iterator<FTVertex> {

        private final ArrayList<StackItem> stack;
        private final BitSet visitedNodes;

        public PostOrderIterator(FTGraph graph) {
            stack = new ArrayList<StackItem>(graph.numberOfColors()+1);
            visitedNodes = new BitSet(graph.numberOfVertices());
            stack.add(new StackItem(graph.getRoot(), 0));
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public FTVertex next() {
            if (!hasNext()) throw new NoSuchElementException();
            return nextVertex();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private FTVertex nextVertex() {
            StackItem v = stack.get(stack.size()-1);
            while (v.edgeId < v.numberOfEdges) {
                FTVertex w;
                do {
                    w = v.vertex.outgoingEdges[v.edgeId++].to;
                } while (visitedNodes.get(w.index) && v.edgeId < v.numberOfEdges);
                if (v.edgeId >= v.numberOfEdges) {
                    // found inner vertex which children are still iterated
                    stack.remove(stack.size()-1);
                    return v.vertex;
                }
                v = new StackItem(w, 0);
                stack.add(v);
            }
            // found leaf
            stack.remove(stack.size()-1);
            return v.vertex;
        }

    }

    public static class PostOrderColorRestrictedIterator implements Iterator<FTVertex> {

        private final ArrayList<StackItem> stack;
        private final BitSet visitedNodes;
        private final BitSet restrictetColors;

        public PostOrderColorRestrictedIterator(FTGraph graph, BitSet restrictetColors) {
            stack = new ArrayList<StackItem>(graph.numberOfColors()+1);
            visitedNodes = new BitSet(graph.numberOfVertices());
            stack.add(new StackItem(graph.getRoot(), 0));
            this.restrictetColors = restrictetColors;
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public FTVertex next() {
            if (!hasNext()) throw new NoSuchElementException();
            return nextVertex();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private FTVertex nextVertex() {
            StackItem v = stack.get(stack.size()-1);
            while (v.edgeId < v.numberOfEdges) {
                FTVertex w;
                do {
                    w = v.vertex.outgoingEdges[v.edgeId++].to;
                } while ((visitedNodes.get(w.index) || !restrictetColors.get(w.getColor())) && v.edgeId < v.numberOfEdges);
                if (v.edgeId >= v.numberOfEdges) {
                    // found inner vertex which children are still iterated
                    stack.remove(stack.size()-1);
                    return v.vertex;
                }
                v = new StackItem(w, 0);
                stack.add(v);
            }
            // found leaf
            stack.remove(stack.size()-1);
            return v.vertex;
        }

    }

    private final static class StackItem {
        private FTVertex vertex;
        private int edgeId;
        private int numberOfEdges;

        private StackItem(FTVertex vertex, int edgeId) {
            set(vertex, edgeId);
        }

        private void set(FTVertex vertex, int edgeId) {
            this.vertex = vertex;
            this.edgeId = edgeId;
            this.numberOfEdges = vertex.numberOfOutgoingEdges();
        }

        private boolean isLeaf(BitSet visited) {
            for (int k=edgeId; k < numberOfEdges; ++k) {
                if (!visited.get(vertex.outgoingEdges[k].to.index)) return false;
            }
            return true;
        }
    }



}
