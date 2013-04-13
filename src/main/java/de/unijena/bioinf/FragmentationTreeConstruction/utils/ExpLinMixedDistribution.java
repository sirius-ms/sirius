package de.unijena.bioinf.FragmentationTreeConstruction.utils;


import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.OutOfRangeException;

public class ExpLinMixedDistribution implements RealDistribution {

    private final double linearScale, quadraticScale, a, b, lambda, offset, minScore;

    public ExpLinMixedDistribution(int offset, double linearScale, double quadraticScale, double a,
    		double b, double lambda) {
    	this.offset = offset;
    	this.linearScale = linearScale;
    	this.quadraticScale = quadraticScale;
    	this.a = a;
    	this.b = b;
    	this.lambda = lambda;
    	this.minScore = linearScale * (a + b*offset);
    }

    @Override
    public double probability(double x) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double density(double x) {
        if (x < offset) {
        	return (a + b * x) * linearScale;
        } else {
        	x-=offset;
        	return Math.min(minScore, lambda * Math.exp(-lambda * x) * quadraticScale);
        }
    }

    @Override
    public double cumulativeProbability(double x) {
    	throw new UnsupportedOperationException();
    }

    @Override
    public double cumulativeProbability(double x0, double x1) throws NumberIsTooLargeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public double inverseCumulativeProbability(double p) throws OutOfRangeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getNumericalMean() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getNumericalVariance() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getSupportLowerBound() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getSupportUpperBound() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSupportLowerBoundInclusive() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSupportUpperBoundInclusive() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSupportConnected() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reseedRandomGenerator(long seed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double sample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double[] sample(int sampleSize) {
        throw new UnsupportedOperationException();
    }
}
