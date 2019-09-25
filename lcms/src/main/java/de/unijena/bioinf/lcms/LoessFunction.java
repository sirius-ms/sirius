package de.unijena.bioinf.lcms;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

public class LoessFunction implements UnivariateFunction {

    private PolynomialSplineFunction loessInterpolator;

    public LoessFunction(PolynomialSplineFunction loessInterpolator) {
        this.loessInterpolator = loessInterpolator;
    }

    @Override
    public double value(double v) {
        if (loessInterpolator.isValidPoint(v))
            return loessInterpolator.value(v);
        else return v;
    }
}
