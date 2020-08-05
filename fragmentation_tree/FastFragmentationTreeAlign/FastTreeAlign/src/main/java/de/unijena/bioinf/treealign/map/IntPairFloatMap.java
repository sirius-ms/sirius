
package de.unijena.bioinf.treealign.map;

public interface IntPairFloatMap extends MapInspectable {

    public final static float DEFAULT_VALUE = 0;

    public enum ReturnType {LOWER, NOT_EXIST, GREATER};

    public int size();
    public boolean isEmpty();
    public float get(int A, int B);
    public void put(int A, int B, float value);

    /**
     *
     * @param A
     * @param B
     * @param value
     * @return LOWER if value is lower than value in map, OR value is lower than DEFAULT_VALUE!!! (if exist).
     *         NOT_EXIST if key did not exist and was inserted
     *         GREATER if value is greater than the old value
     */
    public ReturnType putIfGreater(int A, int B, float value);

    /*
    public IntPairFloatIterator entries();
    */
    public IntPairFloatIterator entries();


}
