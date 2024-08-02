/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */
package de.unijena.bioinf.ChemistryBase.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static de.unijena.bioinf.ChemistryBase.utils.Utils.pairsHalfNoDiag;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class UtilsTest {
    @ParameterizedTest
    @MethodSource
    public void pairsHalfNoDiagTest(List<Integer> ints, List<Pair<Integer,Integer>> pairsExp) {
        List<Pair<Integer, Integer>> pairs = pairsHalfNoDiag(ints, ints.size());
        assertEquals(pairsExp, pairs);
    }

    static Stream<Arguments> pairsHalfNoDiagTest() {
        return Stream.of(
                Arguments.of(Collections.EMPTY_LIST, Collections.EMPTY_LIST),
                Arguments.of(List.of(1), Collections.EMPTY_LIST),
                Arguments.of(List.of(1, 2), List.of(Pair.of(1, 2))),
                Arguments.of(List.of(1, 2, 3), List.of(Pair.of(1, 2), Pair.of(1, 3), Pair.of(2, 3))),
                Arguments.of(List.of(1, 2, 3, 4), List.of(Pair.of(1, 2), Pair.of(1, 3), Pair.of(1, 4), Pair.of(2, 3), Pair.of(2, 4), Pair.of(3, 4))),
                Arguments.of(List.of(1, 2, 3, 4, 5), List.of(Pair.of(1, 2), Pair.of(1, 3), Pair.of(1, 4), Pair.of(1, 5), Pair.of(2, 3), Pair.of(2, 4), Pair.of(2, 5), Pair.of(3, 4), Pair.of(3, 5), Pair.of(4, 5)))
        );
    }
}
