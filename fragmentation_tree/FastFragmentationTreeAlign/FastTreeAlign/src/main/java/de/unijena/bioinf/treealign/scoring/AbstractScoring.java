/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.treealign.scoring;

import java.util.Iterator;

/**
 * Simple symetric singlejoin scoring with constant gap scores
 * @author Kai Dührkop
 */
public abstract class AbstractScoring<T> implements Scoring<T> {
    
    protected abstract float joinOperation(T parent, T join, T other);
    protected abstract float matchOperation(T left, T right);
    protected abstract float gapScore();

    @Override
    public float joinLeft(T left, T join, T right) {
        return joinOperation(left, join, right);
    }

    @Override
    public float joinRight(T right, T join, T left) {
        return joinOperation(right, join, left);
    }

    @Override
    public float match(T left, T right) {
        return matchOperation(left, right);
    }

    @Override
    public float deleteLeft(T left) {
        return gapScore();
    }

    @Override
    public float deleteRight(T right) {
        return gapScore();
    }

    @Override
    public float join(Iterator<T> leftNodes, Iterator<T> rightNodes, int leftSize, int rightSize) {
        throw new UnsupportedOperationException("This scoring doesn't support multijoin scoring");
    }

    @Override
    public float scoreVertices(T left, T right) {
        return 0;
    }
}
