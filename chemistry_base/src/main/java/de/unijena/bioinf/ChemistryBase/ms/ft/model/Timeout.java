package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;

/**
 * This configurations define a timeout for the tree computation. As the underlying problem is NP-hard, it might take
 * forever to compute trees for very challenging (e.g. large mass) compounds. Setting an time constraint allow the program
 * to continue with other instances and just skip the challenging ones.
 * Note that, due to multithreading, this time constraints are not absolutely accurate.
 */
public class Timeout implements Ms2ExperimentAnnotation {

    public final static Timeout NO_TIMEOUT = new Timeout(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private final int numberOfSecondsPerDecomposition, numberOfSecondsPerInstance;

    /**
     * @param secondsPerInstance Set the maximum number of seconds for computing a single compound. Set to 0 to disable the time constraint.
     * @param secondsPerTree Set the maximum number of seconds for a single molecular formula check. Set to 0 to disable the time constraint
     */
    @DefaultInstanceProvider
    public static Timeout newInstance(
            @DefaultProperty int secondsPerInstance,
            @DefaultProperty int secondsPerTree
    ) {
        return new Timeout(secondsPerInstance, secondsPerTree);
    }

    public static Timeout newTimeout(int numberOfSecondsPerInstance, int numberOfSecondsPerDecomposition) {
        if (numberOfSecondsPerDecomposition <= 0) numberOfSecondsPerDecomposition = Integer.MAX_VALUE;
        if (numberOfSecondsPerInstance <= 0) numberOfSecondsPerInstance = Integer.MAX_VALUE;
        if ((numberOfSecondsPerDecomposition == Integer.MAX_VALUE) && (numberOfSecondsPerInstance == Integer.MAX_VALUE))
            return NO_TIMEOUT;
        return new Timeout(numberOfSecondsPerInstance, numberOfSecondsPerDecomposition);
    }

    private Timeout(int numberOfSecondsPerInstance, int numberOfSecondsPerDecomposition) {
        if (numberOfSecondsPerDecomposition != Integer.MAX_VALUE && numberOfSecondsPerDecomposition > numberOfSecondsPerInstance)
            throw new IllegalArgumentException("Timeout for single decomposition is larger than for the whole instance: number of seconds per instance = " + numberOfSecondsPerInstance + ", number of seconds per decomposition = " + numberOfSecondsPerDecomposition);
        this.numberOfSecondsPerInstance = numberOfSecondsPerInstance <= 0 ? Integer.MAX_VALUE : numberOfSecondsPerInstance;
        this.numberOfSecondsPerDecomposition = numberOfSecondsPerDecomposition <= 0 ? Integer.MAX_VALUE : numberOfSecondsPerDecomposition;
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

    public static Timeout none() {
        return NO_TIMEOUT;
    }
}
