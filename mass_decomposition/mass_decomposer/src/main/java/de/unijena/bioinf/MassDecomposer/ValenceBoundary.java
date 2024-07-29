
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

import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.util.Arrays;
import java.util.Map;

/**
 * This class provides an estimation for the maximal number of valence-1 characters in a valid decomposition.
 * It should be used together with ValenceValidator.
 * A decomposition forms a full connected graph if the number of valence-1 vertices is bounded by the number
 * of open valences. For example: A graph with 20 Hydrogen needs 9 Carbon to be fully connected. Each carbon
 * has valence 4, therefore 9 carbon have valence 36. A fully connected graph has n-1 edges for n vertices.
 * Therefore, for 9 carbons 8 edges are needed, leading to 36-(8*2)=20 free valences - enough for 20 Hydrogens.
 *
 * You could say: For each two hydrogen you need one carbon to saturate the open valences (while at least one carbon is
 * free). Therefore, you can bound the number of hydrogens in a molecule with mass M by M+m(C) / (m(H)+m(C)/2)
 * where m(C) and m(H) are the masses of carbon and hydrogen.
 *
 * More general, you can define the minimal weight per free valence , called Q. Now for each character x with
 * valence 1, its maximal abundance in a fully connected graph is M+Q / (m(x) + Q/2)
 * with Q = min_{c_i in Alphabet with valence(c_i) {@literal >} 1} {m(c_i) / (valence(c_i)/2 - 1)}
 *        = minimal number of free edges which can be inserted into the graph after inserting a mass Q
 */
public class ValenceBoundary<T> {

    private final ValencyAlphabet<T> alphabet;
    private final double Q;
    private final int[] singleValenceCharacters;

    /**
     * @param alphabet
     */
    public ValenceBoundary(ValencyAlphabet<T> alphabet) {
        this.alphabet = alphabet;
        final int[] buffer = new int[alphabet.size()];
        int k=0;
        double minQ = Double.POSITIVE_INFINITY;
        for (int i=0; i < alphabet.size(); ++i) {
            final int v = alphabet.valenceOf(i);
            if (v == 1) {
                buffer[k++] = i;
            } else if (v > 1) {
                minQ = Math.min(minQ, alphabet.weightOf(i)/(v/2.0 - 1));
            }
        }
        this.Q = minQ;
        this.singleValenceCharacters = Arrays.copyOf(buffer, k);
    }

    /**
     * returns the maximal number of characters c_index which can be contained in a decomposition of the given mass
     * @param index index of the character
     * @param mass mass of the decomposition
     * @return
     */
    public int calculateBoundaryFor(int index, double mass) {
        return (int)Math.ceil((mass+Q) / (alphabet.weightOf(index) + Q/2));
    }

    /**
     * @see #calculateBoundaryFor(int, double)
     */
    public int calculateBoundaryFor(T elem, double mass) {
        return calculateBoundaryFor(alphabet.indexOf(elem), mass);
    }

    /**
     * Can be used as parameter in {@link de.unijena.bioinf.MassDecomposer.MassDecomposer#decompose(double, Deviation, Map)}
     * @param mass mass of decomposition
     * @return a map which maps the valence-1 characters of the alphabet to their maximal abundance
     */
    public Map<T, Interval> getMapFor(double mass) {
        return getMapFor(mass, null);
    }

    /**
     * Creates a new boundary map which combines the boundaries of the valence boundary and the given boundary map.
     * It uses always the maximal min value and the minimal max values of both maps.
     * @see #getMapFor(double)
     * @param mass mass of decomposition
     * @param boundary a boundary map which maps arbitrary characters of the alphabet to their maximal abundance
     * @return a combined boundary map
     */
    public Map<T, Interval> getMapFor(double mass, Map<T, Interval> boundary) {
        final int n = boundary == null ? 0 : boundary.size();
        final Map<T, Interval> map = alphabet.toMap();
        if (boundary != null) {
            for (Map.Entry<T, Interval> elem : boundary.entrySet()) {
                map.put(elem.getKey(), elem.getValue());
            }
        }
        for (int i=0; i < singleValenceCharacters.length; ++i) {
            final T elem = alphabet.get(singleValenceCharacters[i]);
            final Interval value = map.get(elem);
            final int newValue = calculateBoundaryFor(singleValenceCharacters[i], mass);
            map.put(elem, (value == null) ? new Interval(0, newValue)
                                          : new Interval(value.getMin(), Math.min(value.getMax(), newValue)));
        }
        return map;
    }

}
