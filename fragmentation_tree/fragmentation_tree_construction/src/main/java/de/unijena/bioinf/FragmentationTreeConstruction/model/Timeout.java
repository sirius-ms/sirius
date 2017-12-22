package de.unijena.bioinf.FragmentationTreeConstruction.model;

/**
 * If this annotation is set, Tree Builder will stop after reaching the given number of seconds
 */
public class Timeout {

    public final static Timeout NO_TIMEOUT = new Timeout(0, 0);

    private final int numberOfSecondsPerInstance, numberOfSecondsPerDecomposition;

    public static Timeout newTimeout(int numberOfSecondsPerInstance, int numberOfSecondsPerDecomposition) {
        if (numberOfSecondsPerDecomposition == 0 && numberOfSecondsPerInstance == 0)
            return NO_TIMEOUT;
        return new Timeout(numberOfSecondsPerInstance, numberOfSecondsPerDecomposition);
    }

    private Timeout(int numberOfSecondsPerInstance, int numberOfSecondsPerDecomposition) {
        if (numberOfSecondsPerDecomposition > numberOfSecondsPerInstance && numberOfSecondsPerInstance != 0)
            throw new IllegalArgumentException("Timeout for single decomposition is larger than for the whole instance: number of seconds per instance = " + numberOfSecondsPerInstance + ", number of seconds per decomposition = " + numberOfSecondsPerDecomposition);
        this.numberOfSecondsPerInstance = numberOfSecondsPerInstance;
        this.numberOfSecondsPerDecomposition = numberOfSecondsPerDecomposition;
    }


    public int getNumberOfSecondsPerInstance() {
        return numberOfSecondsPerInstance;
    }

    public int getNumberOfSecondsPerDecomposition() {
        return numberOfSecondsPerDecomposition;
    }
}
