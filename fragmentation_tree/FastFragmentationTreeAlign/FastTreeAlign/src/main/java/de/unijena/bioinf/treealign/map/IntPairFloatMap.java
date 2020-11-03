
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

package de.unijena.bioinf.treealign.map;

public interface IntPairFloatMap extends MapInspectable {

    public final static float DEFAULT_VALUE = 0;

    public enum ReturnType {LOWER, NOT_EXIST, GREATER};

    public int size();
    public boolean isEmpty();
    public float get(int A, int B);
    public void put(int A, int B, float value);

    /**
     *
     * @param A
     * @param B
     * @param value
     * @return LOWER if value is lower than value in map, OR value is lower than DEFAULT_VALUE!!! (if exist).
     *         NOT_EXIST if key did not exist and was inserted
     *         GREATER if value is greater than the old value
     */
    public ReturnType putIfGreater(int A, int B, float value);

    /*
    public IntPairFloatIterator entries();
    */
    public IntPairFloatIterator entries();


}
