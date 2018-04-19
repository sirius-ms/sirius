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

public class SubtreeCounterTest {

    @Test
    public void testSelfPath() {
        TestTree s, t;
        s = t = node("R",
                    node("A"),
                    node("B")
        );
        assertEquals(3, align(s, t));
        s = node("R",
                node("A",
                        node("B")));
        t = node("R",
                node("B",
                        node("A")));
        assertEquals(2, align(s, t));
        t = s;
        assertEquals(3, align(s, t));
        s = node("R",
                node("a"),
                node("b",
                        node("c")
                )
        );
        t = s;
        // TODO: Nachrechnen =(
        assertEquals(6, align(s, t));

        s = node("R",
                node("a"),
                node("b",
                        node("a")
                )
        );
        t = s;
        assertEquals(8, align(s, t));




    }

    private long align(TestTree a , TestTree b) {
        return new DPSubtreeCounter<TestTree>(TestTree.getScoring(), a, b, TestTree.getAdapter()).compute();
    }

}
