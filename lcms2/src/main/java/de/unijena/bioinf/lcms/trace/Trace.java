package de.unijena.bioinf.lcms.trace;

import com.google.common.collect.Range;

/**
 * A trace is a vector of m/z and intensity values in a sample. A trace can span the complete sample, or it is only
 * defined in a predefined region. Indexing is always absolute, with 0 is the index of the first scan in the sample.
 */
public interface Trace {

    /**
     * @return the absolute id this trace starts at
     */
    public int startId();

    /**
     * @return absolute Id this trace ends (inclusive)
     */
    public int endId();

    /**
     * @return the absolute Id of the scan point with the highest intensity within this trace
     */
    public int apex();

    /**
     * @return m/z value at index
     */
    public double mz(int index);

    /**
     * @return weighted average of all m/z values in this trace (or only the most intensive ones)
     */
    public double averagedMz();

    public double minMz();

    public double maxMz();

    /**
     * @return intensity value at index
     */
    public float intensity(int index);

    /**
     * @return scan point ID at the given index
     */
    public int scanId(int index);

    /**
     * @return retention time in seconds at the given index
     */
    public double retentionTime(int index);

    public default float apexIntensity() {
        return intensity(apex());
    }

    /**
     * @return true if the index is within the defined span
     */
    public default boolean inRange(int index) {
        return index >= startId() && index <= endId();
    }

    /**
     * @return number of scan points in this trace
     */
    public default int length() {
        return endId() - startId() + 1;
    }

    public default Rect rectWithIds() {
        return new Rect((float)minMz(), (float)maxMz(), startId(), endId(), averagedMz());
    }
    public default Rect rectWithRts() {
        return new Rect((float)minMz(), (float)maxMz(), retentionTime(startId()), retentionTime(endId()), averagedMz());
    }

    public default float intensityOrZero(int id) {
        return inRange(id) ? intensity(id) : 0f;
    }

}
