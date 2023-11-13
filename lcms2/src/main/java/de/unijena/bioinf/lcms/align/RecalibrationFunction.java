package de.unijena.bioinf.lcms.align;

import com.google.common.collect.Range;
import de.unijena.bioinf.lcms.LoessFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

public class RecalibrationFunction implements UnivariateFunction {

    private final Range<Double> loessDomain;
    private final PolynomialSplineFunction loessFunction;
    private final UnivariateFunction linearBackup;

    public RecalibrationFunction(Range<Double> loessDomain, PolynomialSplineFunction loessFunction, UnivariateFunction linearBackup) {
        this.loessDomain = loessDomain;
        this.loessFunction = loessFunction;
        this.linearBackup = linearBackup;
    }

    @Override
    public double value(double x) {
        if (loessDomain.contains(x)) return loessFunction.value(x);
        else return linearBackup.value(x);
    }
}
