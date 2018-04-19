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
package de.unijena.bioinf.counting;

import org.junit.Test;

import static de.unijena.bioinf.counting.TestTree.node;
import static org.junit.Assert.assertEquals;

public class WeightCounterTest {

    @Test
    public void testUnitCount() {
        TestTree t =
        node("r",
                node("a",
                    node("b"),
                    node("c"),
                    node("d",
                            node("e"),
                            node("f")
                    )
                )
        );
        assertEquals(26, (int)alignUnit(t, t));
        t = node("a", node("b", node("c", node("d"))));
        assertEquals(12, (int)alignUnit(t, t));


    }

    private double alignUnit(TestTree a , TestTree b) {
        final Weighting<TestTree> weights = new Weighting<TestTree>() {
            @Override
            public double weight(TestTree u, TestTree v) {
                return 1;
            }
        };
        return new WeightedPathCounting<TestTree>(TestTree.getScoring(), weights, a, b, TestTree.getAdapter()).compute();
    }

}
