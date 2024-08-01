
package de.unijena.bioinf.treealign.map;

import org.junit.Test;

import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Kai Dührkop
 */
public class IntPairFloatMapTest {

    private final static float DELTA = Float.MIN_NORMAL;

    @Test
    public void testHashMapAllocation() {
        final IntPairFloatHashMap map = new IntPairFloatHashMap(0);
        final IntPairFloatHashMap map2 = new IntPairFloatHashMap(1);
        final IntPairFloatHashMap map3 = new IntPairFloatHashMap(1000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongHashMapAllocation() {
        final IntPairFloatHashMap map = new IntPairFloatHashMap(-1);
    }

    @Test
    public void testIteratingMap() {
        final IntPairFloatHashMap map = new IntPairFloatHashMap(2);
        final int[] As = new int[]{138, 867, 47, 520, 540, 456, 352, 312, 822,
                381, 778, 833, 233, 870, 943, 934, 389, 929, 902, 305};
        final int[] Bs = new int[]{758, 829, 644, 473, 668, 56, 796, 188, 785,
                470, 801, 870, 553, 894, 322, 408, 933, 487, 817, 925};
        final float[] values = new float[]{286.67f, 34.67f, 70.00f, 72.25f, 20.88f, 18.59f, 28.44f, 0.19f,
                0.43f, 12.56f, 21.00f, 1.07f, 10.75f, 11.68f, 4.20f, 12.78f, 0.85f, 9.37f, 2.73f, 34.86f};
        for (int i=0; i < As.length; ++i) {
            map.put(As[i], Bs[i], values[i] * 0.123f);
            map.put(As[i], Bs[i], values[i]);
        }
        for (int i=0; i < As.length; ++i) {
            assertEquals(values[i], map.get(As[i], Bs[i]), DELTA);
        }
        
        final IntPairFloatIterator iter = map.entries();
        int k = 0;
        while (iter.hasNext()) {
            iter.next();
            final int a = iter.getLeft();
            final int b = iter.getRight();
            final float v = iter.getValue();
            final int i = find(As, Bs, a, b);
            assertTrue(i >= 0);
            assertEquals(0, (k & (1<<i)));
            k |= 1<<i;
            assertEquals(values[i], v, DELTA);
        }
        assertEquals(((1<<As.length)-1), k);
    }
    
    private int find(int[] as, int[] bs, int a, int b) {
        for (int i=0; i < as.length; ++i) {
            if (as[i] == a && bs[i] == b) {
                return i;
            }
        }
        return -1;
    }

    @Test(expected = NoSuchElementException.class)
    public void TestUninitializedIterator() {
        final IntPairFloatHashMap map = new IntPairFloatHashMap(10);
        map.put(1, 3, 1f);
        map.put(2, 5, 2f);
        final IntPairFloatIterator iter = map.entries();
        iter.getLeft();
    }

    @Test(expected = NoSuchElementException.class)
    public void TestEmptyIterator() {
        final IntPairFloatHashMap map = new IntPairFloatHashMap(10);
        final IntPairFloatIterator iter = map.entries();
        assertEquals(iter.hasNext(), false);
        iter.getValue();
    }
    
}
