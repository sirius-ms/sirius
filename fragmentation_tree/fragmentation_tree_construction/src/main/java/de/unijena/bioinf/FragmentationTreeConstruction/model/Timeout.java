package de.unijena.bioinf.FragmentationTreeConstruction.model;

/**
 * If this annotation is set, Tree Builder will stop after reaching the given number of seconds
 */
public class Timeout {

    public final static Timeout NO_TIMEOUT = new Timeout(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private final int numberOfSecondsPerInstance, numberOfSecondsPerDecomposition;

    public static Timeout newTimeout(int numberOfSecondsPerInstance, int numberOfSecondsPerDecomposition) {
        if (numberOfSecondsPerDecomposition<0) numberOfSecondsPerDecomposition = Integer.MAX_VALUE;
        if (numberOfSecondsPerInstance<0) numberOfSecondsPerInstance = Integer.MAX_VALUE;
        if ((numberOfSecondsPerDecomposition == Integer.MAX_VALUE) && (numberOfSecondsPerInstance == Integer.MAX_VALUE))
            return NO_TIMEOUT;
        return new Timeout(numberOfSecondsPerInstance, numberOfSecondsPerDecomposition);
    }

    private Timeout(int numberOfSecondsPerInstance, int numberOfSecondsPerDecomposition) {
        if (numberOfSecondsPerDecomposition!=Integer.MAX_VALUE && numberOfSecondsPerDecomposition > numberOfSecondsPerInstance)
            throw new IllegalArgumentException("Timeout for single decomposition is larger than for the whole instance: number of seconds per instance = " + numberOfSecondsPerInstance + ", number of seconds per decomposition = " + numberOfSecondsPerDecomposition);
        this.numberOfSecondsPerInstance = numberOfSecondsPerInstance<0 ? Integer.MAX_VALUE : numberOfSecondsPerInstance;
        this.numberOfSecondsPerDecomposition = numberOfSecondsPerDecomposition < 0 ? Integer.MAX_VALUE : numberOfSecondsPerDecomposition;
    }

    public boolean hasTimeout() {
        return numberOfSecondsPerDecomposition < Integer.MAX_VALUE || numberOfSecondsPerInstance < Integer.MAX_VALUE;
    }


    public int getNumberOfSecondsPerInstance() {
        return numberOfSecondsPerInstance;
    }

    public int getNumberOfSecondsPerDecomposition() {
        return numberOfSecondsPerDecomposition;
    }
}
