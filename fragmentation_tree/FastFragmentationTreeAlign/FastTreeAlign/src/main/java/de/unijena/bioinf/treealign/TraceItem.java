
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

import java.util.List;

public class TraceItem<T> {
    public final int A;
    public final int B;
    public final Tree<T> u;
    public final Tree<T> v;

    public TraceItem(Tree<T> u, Tree<T> v, int a, int b) {
        A = a;
        B = b;
        this.u = u;
        this.v = v;
    }

    public TraceItem(Tree<T> u, Tree<T> v, List<Tree<T>> a, List<Tree<T>> b) {
        this(u, v, (1<<a.size())-1, (1<<b.size())-1);
    }

    public TraceItem(Tree<T> u, Tree<T> v) {
        this(u, v, u.children(), v.children());

    }
}
