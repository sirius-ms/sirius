
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

package de.unijena.bioinf.treealign.multijoin;

import de.unijena.bioinf.treealign.Set;
import de.unijena.bioinf.treealign.TraceItem;
import de.unijena.bioinf.treealign.Tree;


/**
 * @author Kai Dührkop
 */
class MultiJoinTraceItem<T> extends TraceItem<T> {
    
    final short l;
    final short r;

    MultiJoinTraceItem(Tree<T> u, Tree<T> v, int a, int b, short l, short r) {
        super(u, v, a, b);
        this.l = l;
        this.r = r;
    }

    MultiJoinTraceItem(Tree<T> u, Tree<T> v, int a, int b) {
        this(u, v, a, b, (short) 0, (short) 0);
    }

    MultiJoinTraceItem(Tree<T> u, Tree<T> v) {
        this(u, v, Set.of(u.children()), Set.of(v.children()));
    }
}
