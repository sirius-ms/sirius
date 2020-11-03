
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

package de.unijena.bioinf.treealign.scoring;

import java.util.Iterator;

public interface Scoring<T> {

    /**
     * @return true if scoring should score vertices
     */
    public boolean isScoringVertices();

    /**
     * @param left  parent node. Its incomming edge is joined with one of its outgoing edges
     * @param join  child node. Its incomming edge is joined with the incomming edge of its parent node
     * @param right Its incomming edge is matched with the joined incomming edges of (join, left)
     * @return score for this join operation
     */
    public float joinLeft(T left, T join, T right);

    /**
     * @param left  the incomming edge of this vertex is matched
     * @param right the incomming edge of this vertex is matched
     * @return score for this match operation
     */
    public float match(T left, T right);


    /**
     * @param right parent node. Its incomming edge is joined with one of its outgoing edges
     * @param join  child node. Its incomming edge is joined with the incomming edge of its parent node
     * @param left  Its incomming edge is matched with the joined incomming edges of (join, right)
     * @return score for this join operation
     */
    public float joinRight(T right, T join, T left);

    /**
     * score for deleting left and its incomming edge
     *
     * @param left
     * @return
     */
    public float deleteLeft(T left);

    /**
     * score for deleting right and its incomming edge
     *
     * @param right
     * @return
     */
    public float deleteRight(T right);

    /**
     * Multijoin scoring. Match the join of all left nodes with the join of all rightnodes.
     * The iterators iterates over an immutable(!!!) list of vertices, where the next element is
     * the parent node of the previous element
     *
     * @param leftNodes  iterator of an immutable(!!!) list with left nodes
     * @param rightNodes iterator of an immutable(!!!) list with right nodes
     * @param leftSize   size of the immutable node list
     * @param rightSize  size of the immutable node list
     * @return
     */
    public float join(Iterator<T> leftNodes, Iterator<T> rightNodes, int leftSize, int rightSize);

    /**
     * Fragment score. While match scores the edges and vertices, this scoring scores just the vertex
     *
     * @param left
     * @param right
     * @return
     */
    public float scoreVertices(T left, T right);

    /**
     * Score for aligning the whole tree with itself
     *
     * @param root
     * @return
     */
    public float selfAlignScore(T root);

}
