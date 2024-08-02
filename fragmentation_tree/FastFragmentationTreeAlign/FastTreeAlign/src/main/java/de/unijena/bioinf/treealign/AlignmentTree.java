
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

package de.unijena.bioinf.treealign;

import de.unijena.bioinf.graphUtils.tree.*;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.*;

public class AlignmentTree<T> {

    private final TreeAdapter<T> adapter;
    private Node<T> root;
    private ArrayList<Node<T>> deletionNodes;
    public ArrayList<Node<T>> matchNodes;
    private boolean dirty = false;
    private int joincounter = 0;
    private TObjectDoubleHashMap<T> GAP_COSTS_L, GAP_COSTS_R;

    AlignmentTree(TreeAdapter<T> adapter) {
        this.adapter = adapter;
        this.deletionNodes = new ArrayList<Node<T>>();
        this.matchNodes = new ArrayList<Node<T>>();
        this.GAP_COSTS_L = new TObjectDoubleHashMap<T>();
        this.GAP_COSTS_R = new TObjectDoubleHashMap<T>();
    }

    public Node<T> getRoot() {
        return root;
    }

    public TreeCursor<Node<T>> getCursor() {
        return TreeCursor.getCursor(root);
    }

    public void assignAll() {
        if (!dirty) return;
        dirty = false;
        // asignment nodes
        final HashMap<T, Node<T>> leftMap = new HashMap<T, Node<T>>();
        final HashMap<T, Node<T>> rightMap = new HashMap<T, Node<T>>();
        for (Node<T> node : matchNodes) {
            leftMap.put(node.left, node);
            rightMap.put(node.right, node);
        }
        leftMap.put(root.left, root);
        rightMap.put(root.right, root);
        final HashMap<T, TNode<T>> nodesLeft = decorateTree(root.left);
        final HashMap<T, TNode<T>> nodesRight = decorateTree(root.right);
        /*
        for (Node<T> node : deletionNodes) {
            if (node.left!=null) {
                final Node<T> parent = leftMap.get(nodesLeft.get(node.left).parent.node);
                parent.children.add(node);
                node.parent=parent;
                leftMap.put(node.left, node);
            } else if (node.right!=null) {
                final Node<T> parent = rightMap.get(nodesRight.get(node.right).parent.node);
                parent.children.add(node);
                node.parent=parent;
                rightMap.put(node.right, node);
            } else assert false;
        }
        */
        for (Node<T> node : matchNodes) {
            final Node<T> parentLeft = leftMap.get(nodesLeft.get(node.left).parent.node);
            final Node<T> parentRight = rightMap.get(nodesRight.get(node.right).parent.node);
            if (parentLeft == null || parentRight == null) {
                final HashSet<T> pathLeft = new HashSet<T>();
                final HashSet<T> pathRight = new HashSet<T>();
                TNode<T> u = nodesLeft.get(node.left);
                TNode<T> v = nodesRight.get(node.right);
                while (leftMap.get(u.parent.node) == null) {
                    pathLeft.add(u.parent.node);
                    u = u.parent;
                }
                while (rightMap.get(v.parent.node) == null) {
                    pathRight.add(v.parent.node);
                    v = v.parent;
                }
                Node<T> currentNode = node;
                for (int i = deletionNodes.size() - 1; i >= 0; --i) {
                    final Node<T> deleteNode = deletionNodes.get(i);
                    if (deleteNode.left != null && pathLeft.contains(deleteNode.left)) {
                        deleteNode.children.add(currentNode);
                        assert currentNode.parent == null;
                        currentNode.parent = deleteNode;
                        currentNode = deleteNode;
                        pathLeft.remove(deleteNode.left);
                        leftMap.put(currentNode.left, currentNode);
                    } else if (deleteNode.right != null && pathRight.contains(deleteNode.right)) {
                        deleteNode.children.add(currentNode);
                        assert currentNode.parent == null;
                        currentNode.parent = deleteNode;
                        currentNode = deleteNode;
                        pathRight.remove(deleteNode.right);
                        rightMap.put(currentNode.right, currentNode);
                    }
                    if (pathLeft.size() + pathRight.size() == 0) {
                        if (deleteNode.parent != null) {
                            System.err.println("!");
                        }
                        assert deleteNode.parent == null;
                        final Node<T> candidateLeft = leftMap.get(u.parent.node);
                        final Node<T> candidateRight = rightMap.get(v.parent.node);
                        if (candidateLeft == null || candidateLeft.isAncestorOf(candidateRight)) {
                            deleteNode.parent = candidateRight;
                            candidateRight.children.add(deleteNode);
                        } else if (candidateRight == null || candidateRight.isAncestorOf(candidateLeft)) {
                            deleteNode.parent = candidateLeft;
                            candidateLeft.children.add(deleteNode);
                        } else
                            assert false;
                        break;
                    }
                }
            } else if (parentLeft == parentRight) {
                assert node.parent == null;
                node.parent = parentRight;
                parentRight.children.add(node);
            } else if (parentRight.isAncestorOf(parentLeft)) {
                assert node.parent == null;
                node.parent = parentLeft;
                parentLeft.children.add(node);
            } else if (parentLeft.isAncestorOf(parentRight)) {
                assert node.parent == null;
                node.parent = parentRight;
                parentRight.children.add(node);
                assert parentLeft.isAncestorOf(parentRight);
            } else
                assert false;
        }
        final Iterator<Node<T>> iterator = new PreOrderTraversal.TreeIterator<Node<T>>(getCursor());
        int k = 0;
        while (iterator.hasNext()) {
            final Node<T> u = iterator.next();
            u.index = ++k;
        }
    }

    private HashMap<T, TNode<T>> decorateTree(T root) {
        final HashMap<T, TNode<T>> list = new HashMap<T, TNode<T>>();
        new PostOrderTraversal(root, adapter).call(new PostOrderTraversal.Call<T, TNode<T>>() {
            @Override
            public TNode<T> call(T vertex, List<TNode<T>> values, boolean isRoot) {
                final TNode<T> node = new TNode<T>(vertex);
                for (TNode<T> v : values) v.parent = node;
                node.children = new ArrayList<TNode<T>>(values);
                list.put(vertex, node);
                return node;
            }
        });
        return list;
    }

    private static class TNode<T> {
        private T node;
        private TNode<T> parent;
        private ArrayList<TNode<T>> children;

        private TNode(T node) {
            this.node = node;
        }
    }


    private void addParentNode(Node<T> node, Node<T> parent) {
        assert node != parent;
        if (node.parent == null) {
            node.parent = parent;
            parent.children.add(node);
        } else if (node.parent == parent) {
            // do nothing
        } else {
            Node<T> oldParent = node.parent;
            node.parent = parent;
            oldParent.children.remove(node);
            parent.children.add(node);
            parent.parent = oldParent;
            oldParent.children.add(parent);
        }
    }

    public void setRoot(T a, T b, double score) {
        this.root = new Node<T>(a, b, score);
        dirty = true;
    }

    public void addMatch(T a, T b, double score) {
        dirty = true;
        final Node w = new Node(a, b, score);
        matchNodes.add(w);
    }

    public void addDeletionLeft(T a, double score) {
        //GAP_COSTS_L.put(a, score);
        deletionNodes.add(new Node<T>(a, null, score));
    }

    public void addDeletionRight(T b, double score) {
        //GAP_COSTS_R.put(b, score);
        deletionNodes.add(new Node<T>(null, b, score));
    }

    public void addJoin(Iterator<T> left, Iterator<T> right, int leftSize, int rightSize, double score) {
        dirty = true;
        final T terminalLeft = left.next();
        final T terminalRight = right.next();
        matchNodes.add(new Node<T>(terminalLeft, terminalRight, score, leftSize, rightSize));
    }

    public void addInnerJoinLeft(T node) {
        final Node<T> vertex = new Node<T>(node, null, 0d);
        vertex.inJoin = true;
        deletionNodes.add(vertex);
    }

    public void addInnerJoinRight(T node) {
        final Node<T> vertex = new Node<T>(null, node, 0d);
        vertex.inJoin = true;
        deletionNodes.add(vertex);
    }

    public final static class Node<T> implements TreeType<Node<T>> {
        public final T left, right;
        private final List<Node<T>> children;
        public final int pathLengthOfLeftJoin;
        public final int pathLengthOfRightJoin;
        public boolean inJoin;
        public final double score;
        private Node<T> parent;
        private int index;

        private Node(T left, T right, double score, int joinLeft, int joinRight) {
            this.left = left;
            this.right = right;
            this.children = new ArrayList<Node<T>>();
            this.score = score;
            this.pathLengthOfLeftJoin = joinLeft;
            this.pathLengthOfRightJoin = joinRight;
            this.inJoin = false;
        }

        public boolean isAncestorOf(Node<T> other) {
            while (other != this && other.parent != null) other = other.parent;
            return other == this;
        }

        public Node<T> getParent() {
            return parent;
        }

        public int getIndex() {
            return index;
        }

        private Node(T left, T right, double score) {
            this(left, right, score, 0, 0);
        }

        public boolean isJoin() {
            return inJoin;
        }

        public boolean isJoinTerminalNode() {
            return pathLengthOfLeftJoin + pathLengthOfRightJoin > 0;
        }

        @Override
        public int degree() {
            return children.size();
        }

        @Override
        public List<Node<T>> children() {
            return children;
        }

        @Override
        public String toString() {
            final String l = left == null ? "-" : left.toString();
            final String r = right == null ? "-" : right.toString();
            return "< " + l + "  ||  " + r + ">";
        }
    }

}
