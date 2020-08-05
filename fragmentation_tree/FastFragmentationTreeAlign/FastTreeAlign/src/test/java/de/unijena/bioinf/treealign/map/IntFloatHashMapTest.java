
package de.unijena.bioinf.treealign.map;


import org.junit.Test;

import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Kai Dührkop
 */
public class IntFloatHashMapTest {
    
    private final static float DELTA = Float.MIN_NORMAL;
    
    @Test
    public void testAllocation() {
        final IntFloatHashMap map = new IntFloatHashMap(0);
        final IntFloatHashMap map2 = new IntFloatHashMap(1);
        final IntFloatHashMap map3 = new IntFloatHashMap(1000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongAllocation() {
        new IntFloatHashMap(-1);
    }
    
    @Test
    public void testSize() {
        final IntFloatHashMap map = new IntFloatHashMap(10);
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        map.put(1, 1.0f);
        map.put(2, 1.0f);
        map.put(3, 1.0f);
        assertEquals(3, map.size());
    }

    @Test
    public void testInsertion() {
        final IntFloatHashMap map = new IntFloatHashMap(10);
        map.put(1, 1f);
        map.put(2, 2f);
        map.put(34, 3f);
        assertEquals(1f, map.get(1), DELTA);
        assertEquals(2f, map.get(2), DELTA);
        assertEquals(3f, map.get(34), DELTA);
    }

    @Test
    public void testDoubleKeyInsertion() {
        // should overwrite double keys
        final IntFloatHashMap map = new IntFloatHashMap(10);
        map.put(1, 1f);
        map.put(2, 2f);
        map.put(34, 3f);
        map.put(2, 4f);
        map.put(36, 5f);
        assertEquals(4, map.size());
        assertEquals(4f, map.get(2), DELTA);
        assertEquals(5f, map.get(36), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void dontAllowNullKeys() {
        final IntFloatHashMap map = new IntFloatHashMap(10);
        map.put(0, 1f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void dontAllowNegativeKeys() {
        final IntFloatHashMap map = new IntFloatHashMap(10);
        map.put(-4, 1f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void dontAllowNaNValues() {
        final IntFloatHashMap map = new IntFloatHashMap(10);
        map.put(1, Float.NaN);
    }

    @Test
    public void testIsAHash() {
        assertTrue(new IntFloatHashMap(10).isHash());
    }
    
    @Test
    public void testPutIfGreater() {
        final IntFloatHashMap map = new IntFloatHashMap(10);
        map.put(1, 12f);
        map.put(13, 27f);
        map.put(126, 39f);
        map.putIfGreater(13, 15);
        assertEquals(3, map.size());
        assertEquals(27f, map.get(13), DELTA);
        map.putIfGreater(13, 27f);
        assertEquals(3, map.size());
        map.putIfGreater(13, 30f);
        assertEquals(3, map.size());
        assertEquals(30f, map.get(13), DELTA);
        map.putIfGreater(13, -33f);
        assertEquals(30f, map.get(13), DELTA);
    }

    @Test
    public void testDefaultValue() {
        final IntFloatHashMap map = new IntFloatHashMap(10);
        assertEquals(map.get(3), 0f, DELTA);
        map.put(3, 5f);
        assertEquals(map.get(3), 5f, DELTA);
        assertEquals(map.get(17), 0f, DELTA);
    }
    
    @Test
    public void TestIteratingMap() {
        final IntFloatHashMap map = new IntFloatHashMap(2);
        final int[] keys = new int[]{1, 5, 21, 18, 4, 88, 129, 2, 78, 91, 1200};
        final float[] values = new float[]{3f, 129f, 17f, 1221f, 91f, 121f, 3f, 66f, 111f, 1002f, 7f};
        assertEquals(keys.length, values.length);
        for (int i=0; i < keys.length; ++i) {
            map.put(keys[i], values[i]/3);
            // overwrite value
            map.put(keys[i], values[i]);
        }
        assertEquals(keys.length, map.size());
        final IntFloatIterator iterator = map.entries();
        int k = 0;
        while (iterator.hasNext()) {
            iterator.next();
            final int key = iterator.getKey();
            final int i = find(keys, key);
            assertTrue(i >= 0);
            assertEquals(0, (k & (1<<i)));
            k |= 1<<i;
            assertEquals(iterator.getValue(), values[i], DELTA);
        }
        assertEquals(k, (1<<keys.length)-1);
    }

    private int find(int[] array, int key) {
        for (int i=0; i < array.length; ++i) {
            if (array[i] == key) {
                return i;
            }
        }
        return -1;
    }
    
    @Test(expected = NoSuchElementException.class)
    public void TestUninitializedIterator() {
        final IntFloatHashMap map = new IntFloatHashMap(10);
        map.put(1, 1f);
        map.put(2, 2f);
        final IntFloatIterator iter = map.entries();
        iter.getKey();
    }

    @Test(expected = NoSuchElementException.class)
    public void TestEmptyIterator() {
        final IntFloatHashMap map = new IntFloatHashMap(10);
        final IntFloatIterator iter = map.entries();
        assertEquals(iter.hasNext(), false);
        iter.getKey();
    }


    
}
