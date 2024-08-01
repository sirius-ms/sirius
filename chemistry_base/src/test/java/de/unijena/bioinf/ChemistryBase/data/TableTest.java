
package de.unijena.bioinf.ChemistryBase.data;

import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

import static org.junit.Assert.assertArrayEquals;

public class TableTest {

    final String ARaw =
            "x,a,b,relative,d\n" +
            "m,1,2,3,4\n" +
            "n,5,6,7,8\n" +
            "o,9,8,7,6\n" +
            "p,5,4,3,2";

    final String BRaw =
            "x,a,b,relative,e\n" +
            "m,7,2,3,4\n" +
            "j,5,6,7,8\n" +
            "o,9,8,7,6\n" +
            "p,5,4,3,2";

    final String CRaw =
            "x,a,b,relative,f\n" +
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
        assertArrayEquals(new String[]{"a","b","relative","d","e"}, m.getColHeader());
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
