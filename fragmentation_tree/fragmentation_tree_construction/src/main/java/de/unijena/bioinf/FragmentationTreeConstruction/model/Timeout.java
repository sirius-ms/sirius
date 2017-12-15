package de.unijena.bioinf.FragmentationTreeConstruction.model;

/**
 * If this annotation is set, Tree Builder will stop after reaching the given number of seconds
 */
public class Timeout {

    public final static Timeout NO_TIMEOUT = new Timeout(0,0);

    private final int numberOfSecondsPerInstance, numberOfSecondsPerDecomposition;

    public Timeout(int numberOfSecondsPerInstance, int numberOfSecondsPerDecomposition) {
        if (numberOfSecondsPerDecomposition > numberOfSecondsPerInstance)
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
