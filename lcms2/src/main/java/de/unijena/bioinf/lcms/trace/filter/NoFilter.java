package de.unijena.bioinf.lcms.trace.filter;

public class NoFilter implements Filter {

    @Override
    public double[] apply(double[] src) {
        return src;
    }
}
