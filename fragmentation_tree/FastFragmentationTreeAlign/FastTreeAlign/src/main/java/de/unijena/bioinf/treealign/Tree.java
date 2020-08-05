
package de.unijena.bioinf.treealign;

import de.unijena.bioinf.graphUtils.tree.TreeType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class Tree<S> implements TreeType<Tree<S>> {

    public final S label;
    public final int index;
    public final int key;
    private final ArrayList<Tree<S>> children;
    private final int depth;
    private Tree<S> parent;

    public Tree(S label, int index, int childNum, int depth, List<Tree<S>> children) {
        this.index = index;
        if (childNum < 0) {
            this.key = 0;
        } else {
            this.key = 1 << childNum;
        }
        this.label = label;
        this.children = new ArrayList<Tree<S>>(children);
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    public Tree<S> getParent() {
        return parent;
    }

    public void setParent(Tree<S> parent) {
        this.parent = parent;
    }

    public int getParentIndex() {
        return parent.index;
    }

    public int degree() {
        return children.size();
    }

    public List<Tree<S>> children() {
        return children;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public Iterator<S> eachAncestors(int num) {
        return new AncestorIterator<S>(this, num);
    }

    @Override
    public String toString() {
        return label.toString();
    }

    private static class AncestorIterator<S> implements Iterator<S> {

        private Tree<S> node;
        private int depth;
        private boolean init;

        private AncestorIterator(Tree<S> node, int depth) {
            this.node = node;
            this.depth = depth;
            this.init = false;
            if (node.depth < depth)
                throw new IndexOutOfBoundsException("Node has not enough ancestors to iterate");
        }

        @Override
        public boolean hasNext() {
            return depth >= 0;
        }

        @Override
        public S next() {
            final Tree<S> nextNode;
            if (depth-- < 0) {
                throw new NoSuchElementException();
            }
            if (!init) {
                nextNode = node;
                init = true;
            } else {
                nextNode = node.parent;
                this.node = nextNode;
            }
            return nextNode.label;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}
