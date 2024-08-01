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

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * @author Kai Dührkop
 */
public class PreOrderTraversal<T> implements Iterable<T> {

    public static interface Call<T, R> {
        public R call(R parentResult, T node);
    }

    private final TreeCursor<T> cursor;

    public PreOrderTraversal(T tree, TreeAdapter<T> adapter) {
        this(StackedTreeCursor.create(tree, adapter));
    }

    public PreOrderTraversal(TreeCursor<T> cursor) {
        this.cursor = cursor;
    }

    public <R> R call(Call<T, R> callback) {
        final TreeCursor<T> c = cursor.clone();
        final ArrayDeque<R> stack = new ArrayDeque<R>();
        R lastReturn = callback.call(null, c.node);
        if (c.isLeaf()) return lastReturn;
        stack.push(lastReturn);
        c.gotoFirstChild();
        OuterLoop:
        while (!c.isRoot()) {
            lastReturn = callback.call(stack.peek(), c.node);
            stack.push(lastReturn);
            if (!c.isLeaf()) {
                c.gotoFirstChild();
            } else {
                while (!c.hasNextSibling()) {
                    c.gotoParent();
                    stack.pop();
                    if (c.isRoot()) break OuterLoop;
                }
                stack.pop();
                c.gotoNextSibling();
            }
        }
        return lastReturn;
    }

    @Override
    public Iterator<T> iterator() {
        return new TreeIterator<T>(cursor);
    }

    public static class TreeIterator<T> implements Iterator<T> {
        private TreeCursor<T> cursor;
        public TreeIterator(TreeCursor<T> cursor) {
            this.cursor = cursor.clone();
        }

        @Override
        public boolean hasNext() {
            return cursor != null;
        }

        @Override
        public T next() {
            final T node = cursor.node;
            if (!cursor.isLeaf()) {
                cursor.gotoFirstChild();
            } else if (cursor.hasNextSibling()) {
                cursor.gotoNextSibling();
            } else {
                while (!cursor.isRoot()) {
                    cursor.gotoParent();
                    if (cursor.hasNextSibling()) {
                        cursor.gotoNextSibling();
                        return node;
                    }
                }
                cursor = null;
            }
            return node;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
