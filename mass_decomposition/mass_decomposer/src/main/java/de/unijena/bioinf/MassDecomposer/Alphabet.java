
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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

package de.unijena.bioinf.MassDecomposer;

import java.util.Map;

/**
 * The alphabet for which a given weight is decomposed. An alphabet is a vector c_1..c_k of k characters of Type T.
 * It maps each character to a weight. It supports access by an index as well as by the character itself.
 */
public interface Alphabet<T> {

    /**
     * @return size of the alphabet. Indizes of characters are 0..{@literal <} size
     */
    int size();

    /**
     * @param i index of the character
     * @return weight of character c_i
     */
    double weightOf(int i);

    /**
     * @param i index of the character
     * @return character c_i
     */
    T get(int i);

    /**
     * Maps the character to its index. This operation should be fast, because internally a modified ordered
     * alphabet is used which have to be mapped back to the original alphabet
     * @param character
     * @return the index of the character
     */
    int indexOf(T character);

    /**
     * Creates an empty Map which have to be able to map a character of the alphabet to an arbitrary value.
     * Of course you could return an ordinary HashMap, but because the key space is usually very small and
     * each key can be mapped perfectly by an index you can return a more efficient data structure (e.g. an array)
     * @param <S>
     * @return
     */
    <S> Map<T, S> toMap();

}
