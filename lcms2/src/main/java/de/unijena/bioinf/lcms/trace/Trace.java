package de.unijena.bioinf.lcms.trace;

/**
 * A trace is a vector of m/z and intensity values in a sample. A trace can span the complete sample, or it is only
 * defined in a predefined region. Indexing is always absolute, with 0 is the index of the first scan in the sample.
 */
public interface Trace {

    /**
     * @return the absolute id this trace starts at
     */
    int startId();

    /**
     * @return absolute Id this trace ends (inclusive)
     */
    int endId();

    /**
     * @return the absolute Id of the scan point with the highest intensity within this trace
     */
    int apex();

    /**
     * @return m/z value at index
     */
    double mz(int index);

    /**
     * @return weighted average of all m/z values in this trace (or only the most intensive ones)
     */
    double averagedMz();

    double minMz();

    double maxMz();

    /**
     * @return intensity value at index
     */
    float intensity(int index);

    /**
     * @return scan point ID at the given index
     */
    int scanId(int index);

    /**
     * @return retention time in seconds at the given index
     */
    double retentionTime(int index);

    default float apexIntensity() {
        return intensity(apex());
    }

    /**
     * @return true if the index is within the defined span
     */
    default boolean inRange(int index) {
        return index >= startId() && index <= endId();
    }

    /**
     * @return number of scan points in this trace
     */
    default int length() {
        return endId() - startId() + 1;
    }

    default Rect rectWithIds() {
        return new Rect((float)minMz(), (float)maxMz(), startId(), endId(), averagedMz());
    }
    default Rect rectWithRts() {
        return new Rect((float)minMz(), (float)maxMz(), retentionTime(startId()), retentionTime(endId()), averagedMz());
    }

    default float intensityOrZero(int id) {
        return inRange(id) ? intensity(id) : 0f;
    }
}
