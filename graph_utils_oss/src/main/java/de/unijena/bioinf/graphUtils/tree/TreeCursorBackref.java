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

/**
 * This is a tree cursor for trees which have a reference back to their parent. Therefore, it is more
 * efficient than TreeCursor as it does not need to allocate a stack
 */
class TreeCursorBackref<T> extends TreeCursor<T> {

    private final T subtreeRoot;

    public TreeCursorBackref(T t, BackrefTreeAdapter<T> adapter) {
        super(t, adapter);
        this.subtreeRoot = this.node;
    }

    @Override
    public BackrefTreeAdapter<T> getAdapter() {
        return (BackrefTreeAdapter<T>)adapter;
    }

    @Override
    public TreeCursor<T> clone() {
        return new TreeCursorBackref<T>(node, getAdapter());
    }

    @Override
    public boolean isRoot() {
        return getParent() == null;
    }

    @Override
    public T getNextSibling() {
        return adapter.getChildrenOf(getParent()).get(getCurrentSiblingIndex()+1);
    }

    @Override
    public T getPreviousSibling() {
        return adapter.getChildrenOf(getParent()).get(getCurrentSiblingIndex()-1);
    }

    @Override
    public T getParent() {
        return getAdapter().getParent(node);
    }

    @Override
    public int getCurrentSiblingIndex() {
        return getAdapter().indexOfSibling(node);
    }

    @Override
    public boolean hasNextSibling() {
        return isRoot() ? false : getCurrentSiblingIndex() + 1 < adapter.getDegreeOf(getParent());
    }

    @Override
    public boolean hasPreviousSibling() {
        return getCurrentSiblingIndex() > 0;
    }

    @Override
    public int getDepth() {
        return getAdapter().getDepth(node);
    }
}
