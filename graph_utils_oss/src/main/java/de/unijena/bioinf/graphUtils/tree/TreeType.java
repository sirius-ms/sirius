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

import java.util.List;

/**
 * Generic tree type. Using this interface prevents you from implementing a
 * new TreeAdapter for each own tree implementation. Instead use TreeType.Adapter
 * instead and implement the methods in the tree class itself.
 * @param <T> the class which implements TreeType
 */
public interface TreeType<T extends TreeType<T>> {

    /**
     * In the most cases, this should be children().size(). Nevertheless, some
     * trees maybe have a faster access to the number of children than to the children itself,
     * so this method should be called if you just want the children number.
     * @return number of children of this de.unijena.bioinf.tree node
     */
    public int degree();

    /**
     * {@see TreeAdapter<T>#getDegreeOf(T)}
     */
    public List<T> children();

    /**
     * {@see TreeAdapter<T>#getChildrenOf(T)}
     */
    public static class Adapter<T extends TreeType<T>> implements TreeAdapter<T> {

        public int getDegreeOf(T vertex) {
            return vertex.degree();
        }

        public List<T> getChildrenOf(T vertex) {
            return vertex.children();
        }
    }

}
