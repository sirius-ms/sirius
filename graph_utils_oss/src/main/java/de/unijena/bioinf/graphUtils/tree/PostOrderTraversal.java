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

import java.util.*;

public class PostOrderTraversal<T> implements Iterable<T> {

    private final TreeCursor<T> cursor;
    private final T root;

    public PostOrderTraversal(TreeCursor<T> cursor) {
        this(cursor, null);
    }

    public PostOrderTraversal(TreeCursor<T> cursor, T root) {
        this.cursor = cursor;
        this.root = root;
    }

    @Deprecated
    public PostOrderTraversal(T tree, TreeAdapter<T> adapter) {
        this(new StackedTreeCursor<T>(tree, adapter));
    }

    public static <T extends TreeType<T>> PostOrderTraversal<T> create(T tree) {
        return new PostOrderTraversal<T>(StackedTreeCursor.create(tree));
    }

    public static <T> PostOrderTraversal<T> createSubtreeTraversal(T tree, TreeAdapter<T> adapter) {
        return new PostOrderTraversal<T>(TreeCursor.getCursor(tree, adapter), tree);
    }

    public Iterator<T> iterator() {
        return new TreeIterator<T>(cursor.clone(), root);
    }

    private boolean isRoot(TreeCursor<T> cursor) {
        return cursor.isRoot() || (root!=null && cursor.node==root);
    }

    public void run(Run<T> f) {
        final TreeCursor<T> c = cursor.clone();
        f.cursor = c;
        c.gotoFirstLeaf();
        while (!isRoot(c)) {
            f.run(c.getNode(), false);
            while (c.hasNextSibling()) {
                c.gotoNextSibling();
                c.gotoFirstLeaf();
                f.run(c.getNode(), false);
            }
            c.gotoParent();
        }
        f.run(c.node, true);
    }

    public <R> R call(Call<T, R> f) {
        final TreeCursor<T> c = cursor.clone();
        f.cursor = c;
        final ArrayList<List<R>> stack = new ArrayList<List<R>>();
        stack.add(new ArrayList<R>(1));
        moveDown(c, stack);
        stack.get(stack.size() - 1).add(f.call(c.node, Collections.<R>emptyList(), c.isRoot()));
        while (!isRoot(c)) {
            while (c.hasNextSibling()) {
                c.gotoNextSibling();
                moveDown(c, stack);
                stack.get(stack.size() - 1).add(f.call(c.node, Collections.<R>emptyList(), false));
            }
            c.gotoParent();
            final List<R> args = stack.remove(stack.size() - 1);
            stack.get(stack.size() - 1).add(f.call(c.node, args, stack.size() == 1));
        }
        return stack.get(0).get(0);
    }

    private <R> void moveDown(TreeCursor<T> cursor, ArrayList<List<R>> stack) {
        final TreeAdapter<T> adapter = cursor.getAdapter();
        int degree = adapter.getDegreeOf(cursor.getNode());
        while (degree > 0) {
            cursor.gotoFirstChild();
            stack.add(new ArrayList<R>(degree));
            degree = adapter.getDegreeOf(cursor.getNode());
        }
    }

    public abstract static class Run<T> {
        private TreeCursor<T> cursor;

        public abstract void run(T vertex, boolean isRoot);

        protected TreeCursor<T> getCursor() {
            return cursor.clone();
        }
    }

    public abstract static class Call<T, R> {
        private TreeCursor<T> cursor;

        public abstract R call(T vertex, List<R> values, boolean isRoot);

        protected TreeCursor<T> getCursor() {
            return cursor.clone();
        }
    }

    public static class TreeIterator<T> implements Iterator<T> {
        private final TreeCursor<T> cursor;
        private boolean finished;
        private final T root;

        public TreeIterator(TreeCursor<T> cursor, T root) {
            this.cursor = cursor;
            this.root = root;
            cursor.gotoFirstLeaf();
            this.finished = false;
        }

        public TreeIterator(TreeCursor<T> cursor) {
            this(cursor, null);
        }

        public boolean hasNext() {
            return !finished;
        }

        public T next() {
            if (cursor.isRoot() || cursor.node==root) {
                if (finished) {
                    throw new NoSuchElementException();
                } else {
                    finished = true;
                    return cursor.getNode();
                }
            } else {
                final T node = cursor.getNode();
                if (!cursor.hasNextSibling()) {
                    cursor.gotoParent();
                } else {
                    cursor.gotoNextSibling();
                    cursor.gotoFirstLeaf();
                }
                return node;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }


}
