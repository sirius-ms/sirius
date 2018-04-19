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
package de.unijena.bioinf.treealign;

import java.util.Iterator;

public interface Backtrace<T> {

    public void deleteLeft(float score, T node);

    public void deleteRight(float score, T node);

    public void match(float score, T left, T right);

    public void innerJoinLeft(T node);

    public void innerJoinRight(T node);

    public void join(float score, Iterator<T> left, Iterator<T> right, int leftNumber, int rightNumber);

    public void matchVertices(float score, T left, T right);

}
