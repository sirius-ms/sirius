
package de.unijena.bioinf.treealign.map;

public interface IntFloatMap extends MapInspectable {
    public int size();
    public boolean isEmpty();
    public float get(int A);
    public void put(int A, float value);
    public void putIfGreater(int A, float value);
    public IntFloatIterator entries();

    public final static float DEFAULT_VALUE = 0;

}
