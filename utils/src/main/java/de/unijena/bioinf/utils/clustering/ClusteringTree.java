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

package de.unijena.bioinf.utils.clustering;

import java.util.*;

public class ClusteringTree<T> {

    private TreeNode root;
    private List<TreeNode> leaves;

    public ClusteringTree(T[] elements) {
        root = new TreeNode();
        leaves = new ArrayList<>();
        for (T element : elements) {
            final TreeNode node = new TreeNode(element);
            node.setParent(root);
            root.addChild(node);
            leaves.add(node);
        }
        root.numberOfLeafNodes = leaves.size();
    }

    public List<TreeNode> getLeaves() {
        return new ArrayList<>(leaves);
    }

    public TreeNode getRoot() {
        return root;
    }

    public List<T> getLeafElements(TreeNode node){
        List<T> elements = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(node);
        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            List<TreeNode> children = current.getChildren();
            if (children==null || children.size()==0){
                elements.add(current.getElement());
            } else {
                for (TreeNode child : children) {
                    queue.add(child);
                }
            }
        }
        return elements;
    }

    protected TreeNode mergeNodes(TreeNode node1, TreeNode node2){
        if (!node1.getParent().equals(node2.getParent())) throw new RuntimeException("TreeNodes must have same parent");
        final TreeNode newParent = new TreeNode();
        newParent.addChild(node1);
        newParent.addChild(node2);
        final TreeNode oldParent = node1.getParent();
        newParent.setParent(oldParent);
        node1.setParent(newParent);
        node2.setParent(newParent);
        oldParent.removeChild(node1);
        oldParent.removeChild(node2);
        oldParent.addChild(newParent);

        newParent.numberOfLeafNodes = node1.getNumberOfLeafNodes()+node2.getNumberOfLeafNodes();
        return newParent;
    }

    public class TreeNode {
        private TreeNode parent;
        private T element;
        private Set<TreeNode> children;
        private int numberOfLeafNodes;
        private TreeNode(){
            parent = null;
            element = null;
            numberOfLeafNodes = 0;
            children = new HashSet<>();
        }

        private TreeNode(T element){
            parent = null;
            this.element = element;
            numberOfLeafNodes = 1;
            children = new HashSet<>();
        }

        private void addChild(TreeNode node){
            children.add(node);
        }

        private void setParent(TreeNode node){
            parent = node;
        }

        private boolean removeChild(TreeNode node) {
            return children.remove(node);
        }


        protected int getNumberOfLeafNodes() {
            return numberOfLeafNodes;
        }

        public boolean isRoot(){
            return parent==null;
        }

        public TreeNode getParent() {
            return parent;
        }

        public T getElement() {
            return element;
        }

        public List<TreeNode> getChildren() {
            return new ArrayList<>(children);
        }
    }
}
