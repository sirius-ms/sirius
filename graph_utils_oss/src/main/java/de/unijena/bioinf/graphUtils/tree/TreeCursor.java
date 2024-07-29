/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.graphUtils.tree;

/**
 * This class collects methods to navigate through a tree.
 * @param <T> type of the tree. Should have an adapter which implements TreeAdapter
 */
public abstract class TreeCursor<T> {

    /*
        FACTORIES
     */

    public static <T> TreeCursor<T> getCursor(T object, TreeAdapter<T> adapter) {
        return adapter instanceof BackrefTreeAdapter    ? new TreeCursorBackref<T>(object, (BackrefTreeAdapter<T>)adapter)
                                                        : new StackedTreeCursor<T>(object, adapter);
    }

    public static <T> TreeCursor<T> getCursor(T object, BackrefTreeAdapter<T> adapter) {
        return new TreeCursorBackref<T>(object, adapter);
    }

    @SuppressWarnings("unchecked")
    public static <T extends TreeType<T>> TreeCursor getCursor(T tree) {
        return tree instanceof BackrefTreeType  ? new TreeCursorBackref(tree, new BackrefTreeType.Adapter())
                                                : new StackedTreeCursor(tree, new TreeType.Adapter<T>());
    }



    /**
     * current vertex
     */
    protected T node;

    /**
     * adapter, which gives access to the properties of a generic tree node
     */
    protected final TreeAdapter<T> adapter;

    /**
     * Construct an AbstractTreeCursor for the given tree with the given adapter
     * @param t tree node
     * @param adapter is used to get access to the properties of the tree node
     */
    protected TreeCursor(T t, TreeAdapter<T> adapter) {
        this.node = t;
        this.adapter = adapter;
    }

    /**
     * If you use a implementation of TreeAdapter which is mutable: please don't call a
     * mutable function on it, because this method doesn't return a copy of the adapter but the adapter itself.
     * @return gives access to the (hopefully) immutable tree adapter.
     */
    public TreeAdapter<T> getAdapter() {
        return adapter;
    }

    /**
     * @return a copy of this cursor.
     */
    @Override
    public abstract TreeCursor<T> clone();

    /**
     * @return the first child
     * @throws TreeException if current vertex is a leaf
     */
    public T getFirstChild() {
        return adapter.getChildrenOf(node).get(0);
    }

    /**
     * @return the last child
     * @throws TreeException if current vertex is a leaf
     */
    public T getLastChild() {
        return adapter.getChildrenOf(node).get(adapter.getDegreeOf(node)-1);
    }

    /**
     * @return the current vertex
     */
    public T getNode() {
        return node;
    }

    /**
     * remark: a vertex can be root and leaf at the same time
     * @return true if the current vertex is a leaf (which means: has no children)
     */
    public boolean isLeaf() {
        return adapter.getDegreeOf(node) == 0;
    }

    /**
     * remark: a vertex can be root and leaf at the same time
     * @return true if the current vertex is a root (which means: has no parent)
     */
    public abstract boolean isRoot();

    /**
     * @return the next sibling (means the next child of the parent)
     * @throws TreeException if this vertex has no further siblings
     */
    public abstract T getNextSibling();

    /**
     * @return the previous sibling (means the previous child of the parent)
     * @throws TreeException if this vertex is the first child
     */
    public abstract T getPreviousSibling();

    /**
     * @return the parent of the current vertex
     * @throws TreeException if this vertex is a root
     */
    public abstract T getParent();

    /**
     * @return the index of this vertex in the list of children of the parent
     */
    public abstract int getCurrentSiblingIndex();

    /**
     * set the first child as the current vertex
     */
    public void gotoFirstChild() {
        this.node = getFirstChild();
    }

    /**
     * set the last child as the current vertex
     */
    public void gotoLastChild() {
        this.node = getLastChild();
    }

    /**
     * use the next sibling as current node
     * @throws TreeException if current vertex has no further siblings
     */
    public void gotoNextSibling() {
        this.node = getNextSibling();
    }

    /**
     * use the previous sibling as current node
     * @throws TreeException if current vertex has no previous siblings
     */
    public void gotoPreviousSibling() {
        this.node = getPreviousSibling();
    }

    /**
     * @return true if current vertex has further siblings
     */
    public abstract boolean hasNextSibling();

    /**
     * @return true if current vertex has previous siblings
     */
    public abstract boolean hasPreviousSibling();

    /**
     * use the parent as current node
     * @throws TreeException if current node is a root
     */
    public void gotoParent() {
        this.node = getParent();
    }

    /**
     * moves through the tree from parent to parent until it reaches the root.
     * set current vertex as root.
     */
    public void gotoRoot() {
        while (!isRoot()) gotoParent();
    }

    /**
     * move through the tree until you get a leaf node at the bottom. In each child list choose the first child
     */
    public void gotoFirstLeaf() {
        while (!isLeaf()) gotoFirstChild();
    }

    /**
     * returns the depth of the node
     * a root has a depth of 0. All childs of the root have depth 1 and so on.
     */
    public abstract int getDepth();

    /**
     * move through the tree until you get a leaf node at the bottom. In each child list choose the last child
     */
    public void gotoLastLeaf() {
        while (!isLeaf()) gotoLastChild();
    }

    /**
     * this function traverses the whole sub tree
     * @return the number of vertices in the subtree, rootet by the current vertex
     */
    public int numberOfVertices() {
        int number = 0;
        for (T vertex : new PostOrderTraversal<T>(this)) {
            ++number;
        }
        return number;
    }

    /**
     * this function traverses the whole sub tree
     * @return the maximum degree of vertices in the subtree, rootet by the current vertex
     */
    public int maxDegree() {
        int number = 0;
        for (T vertex : new PostOrderTraversal<T>(this)) {
            number = Math.max(adapter.getDegreeOf(vertex), number);
        }
        return number;
    }

}
