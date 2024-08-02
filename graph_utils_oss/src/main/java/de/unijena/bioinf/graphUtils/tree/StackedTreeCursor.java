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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.graphUtils.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This TreeCursor is used for tree implementations in which vertices have no information about their parents.
 * It uses a stack to navigate back to the parent nodes.
 *
 * @param <T> A class that implements de.unijena.bioinf.tree.TreeType
 */
class StackedTreeCursor<T> extends TreeCursor<T> {

    protected final ArrayList<StackItem<T>> stack;
    protected int currentChildIndex;

    public static <T extends TreeType<T>> StackedTreeCursor<T> create(T tree) {
        return new StackedTreeCursor<T>(tree, new TreeType.Adapter<T>());
    }

    public static <T> StackedTreeCursor<T> create(T tree, TreeAdapter<T> adapter) {
        return new StackedTreeCursor<T>(tree, adapter);
    }

    public StackedTreeCursor(StackedTreeCursor<T> c) {
        super(c.node, c.getAdapter());
        currentChildIndex = c.currentChildIndex;
        stack = new ArrayList<StackItem<T>>(c.stack.size());
        for (StackItem<T> item : c.stack) {
            stack.add(new StackItem<T>(item.node, item.index));
        }
    }

    public StackedTreeCursor(T t, TreeAdapter<T> adapter) {
        super(t, adapter);
        this.stack = new ArrayList<StackItem<T>>();
        this.currentChildIndex = 0;
    }

    @Override
    public StackedTreeCursor<T> clone() {
        return new StackedTreeCursor<>(this);
    }

    public int getMaxDepth() {
        return new PostOrderTraversal<T>(this).call(new PostOrderTraversal.Call<T, Integer>() {
            @Override
            public Integer call(T vertex, List<Integer> values, boolean isRoot) {
                return Math.max(getCursor().getDepth(), values.isEmpty() ? 0 : Collections.max(values));
            }
        });
    }

    @Override
    public boolean isRoot() {
        return stack.isEmpty();
    }

    @Override
    public T getNextSibling() {
        final T v = peekStack().node;
        if (currentChildIndex >= adapter.getDegreeOf(v))
            throw new TreeException("The last child has no further siblings");
        return adapter.getChildrenOf(v).get(currentChildIndex + 1);
    }

    @Override
    public T getPreviousSibling() {
        if (currentChildIndex <= 0) throw new TreeException("The first child has no previous siblings");
        return adapter.getChildrenOf(peekStack().node).get(currentChildIndex - 1);
    }

    @Override
    public void gotoNextSibling() {
        super.gotoNextSibling();
        ++currentChildIndex;
    }

    @Override
    public void gotoPreviousSibling() {
        super.gotoPreviousSibling();
        --currentChildIndex;
    }

    @Override
    public boolean hasNextSibling() {
        if (isRoot()) return false;
        return (currentChildIndex + 1) < adapter.getDegreeOf(peekStack().node);
    }

    @Override
    public boolean hasPreviousSibling() {
        if (isRoot()) return false;
        return currentChildIndex > 0;
    }

    @Override
    public void gotoFirstChild() {
        pushStack(node, currentChildIndex);
        currentChildIndex = 0;
        super.gotoFirstChild();
    }

    @Override
    public void gotoLastChild() {
        pushStack(node, currentChildIndex);
        currentChildIndex = adapter.getDegreeOf(node);
        super.gotoLastChild();
    }

    @Override
    public void gotoParent() {
        StackItem<T> item = popStack();
        this.node = item.node;
        this.currentChildIndex = item.index;
    }

    /**
     * root has depth 1, each other vertex has depth of its parent + 1
     */
    @Override
    public int getDepth() {
        return stack.size();
    }

    @Override
    public T getParent() {
        return peekStack().node;
    }

    @Override
    public int getCurrentSiblingIndex() {
        return currentChildIndex;
    }


    protected void pushStack(T node, int index) {
        stack.add(new StackItem<T>(node, index));
    }

    protected StackItem<T> popStack() {
        final StackItem<T> node = peekStack();
        stack.remove(stack.size() - 1);
        return node;
    }

    protected StackItem<T> peekStack() {
        if (stack.isEmpty()) {
            throw new TreeException("root '" + node + "' has no parent node");
        }
        return stack.get(stack.size() - 1);
    }

    private final static class StackItem<T> {
        private final int index;
        private final T node;

        private StackItem(T node, int index) {
            this.node = node;
            this.index = index;
        }
    }


}
