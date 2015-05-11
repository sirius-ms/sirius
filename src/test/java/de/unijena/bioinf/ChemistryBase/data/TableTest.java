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
package de.unijena.bioinf.ChemistryBase.data;

import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

import static org.junit.Assert.assertArrayEquals;

public class TableTest {

    final String ARaw =
            "x,a,b,c,d\n" +
            "m,1,2,3,4\n" +
            "n,5,6,7,8\n" +
            "o,9,8,7,6\n" +
            "p,5,4,3,2";

    final String BRaw =
            "x,a,b,c,e\n" +
            "m,7,2,3,4\n" +
            "j,5,6,7,8\n" +
            "o,9,8,7,6\n" +
            "p,5,4,3,2";

    final String CRaw =
            "x,a,b,c,f\n" +
            "n,5,6,4,8\n" +
            "l,1,1,1,1\n" +
            "j,8,6,7,8";

    public static Iterator<String[]> parseTable(String tab) {
        final String[] list = tab.split("\n");
        final String[][] t = new String[list.length][];
        int k=0;
        for (String s : list) t[k++] = s.split(",");
        return Arrays.asList(t).iterator();
    }

    @Test
    public void testOverlay() {
        final DoubleDataMatrix m = DoubleDataMatrix.overlay(Arrays.asList(parseTable(ARaw), parseTable(BRaw)), Arrays.asList(parseTable(CRaw)),
                Arrays.asList("A", "B", "C"), null, -1d);
        assertArrayEquals(new String[]{"m","n","o","p","j"}, m.getRowHeader());
        assertArrayEquals(new String[]{"a","b","c","d","e"}, m.getColHeader());
        final double[] values1 = getValuesFromLayers(m,1,2);
        assertArrayEquals(new double[]{7, -1, 4}, values1, 1e-4);
        final double[] values2 = getValuesFromLayers(m,4,0);
        assertArrayEquals(new double[]{-1, 5, 8}, values2, 1e-4);
        final double[] values3 = getValuesFromLayers(m,0,0);
        assertArrayEquals(new double[]{1, 7, -1}, values3, 1e-4);

    }

    private double[] getValuesFromLayers(DoubleDataMatrix m, int i, int j) {
        return m.getXY(i, j);
    }




}
