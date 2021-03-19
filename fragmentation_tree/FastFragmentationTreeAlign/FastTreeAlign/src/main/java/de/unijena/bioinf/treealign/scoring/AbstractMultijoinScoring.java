
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

import de.unijena.bioinf.util.Iterators;

import java.util.Iterator;

/**
 * @author Kai Dührkop
 */
public abstract class AbstractMultijoinScoring<T> extends AbstractScoring<T> {

    public abstract float join(Iterator<T> leftNodes, Iterator<T> rightNodes, int leftSize, int rightSize);

    protected float joinOperation(T parent, T join, T other) {
        return join(Iterators.pair(join, parent), Iterators.singleton(other), 2, 1);
    }
    
}
