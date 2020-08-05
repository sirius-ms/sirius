
package de.unijena.bioinf.counting;

import de.unijena.bioinf.treealign.Backtrace;
import de.unijena.bioinf.treealign.TreeAlignmentAlgorithm;

public class AlignmentWrapper<T> implements TreeAlignmentAlgorithm<T> {

    private final Object algorithm;

    public AlignmentWrapper(DPPathCounting<T> algorithm) {
        this.algorithm = algorithm;
    }
    public AlignmentWrapper(WeightedPathCounting<T> algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public float compute() {
        if (algorithm instanceof DPPathCounting) {
            final long value = ((DPPathCounting)algorithm).compute();
            if (value > Float.MAX_VALUE) throw new RuntimeException("Return value exceeds float number space");
            return (float)value;
        } else if (algorithm instanceof WeightedPathCounting) {
            final double result = ((WeightedPathCounting)algorithm).compute();
            return (float)result;
        } else throw new IllegalStateException();
    }

    @Override
    public void backtrace(Backtrace<T> tracer) {
        throw new UnsupportedOperationException();
    }
}
