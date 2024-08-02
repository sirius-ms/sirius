package de.unijena.bioinf.lcms.align;

import org.apache.commons.lang3.Range;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

public class RecalibrationFunction implements UnivariateFunction {
    private final Range<Double> loessDomain;
    private final PolynomialSplineFunction loessFunction;
    private final UnivariateFunction linearBackup;


    private static RecalibrationFunction IDENTITY = new RecalibrationFunction(null, new Identity());
    public static RecalibrationFunction identity() {
        return IDENTITY;
    }
    public static RecalibrationFunction linear(PolynomialFunction function) {
        return new RecalibrationFunction(null, function);
    }
    public static RecalibrationFunction loess(PolynomialSplineFunction loess, UnivariateFunction linearBackup) {
        return new RecalibrationFunction(loess, linearBackup);
    }

    private RecalibrationFunction(PolynomialSplineFunction loessFunction, UnivariateFunction linearBackup) {
        this.loessFunction = loessFunction;
        this.linearBackup = linearBackup;
        if (loessFunction!=null) {
            double[] knots = loessFunction.getKnots();
            loessDomain = Range.of(
                knots[0], knots[knots.length-1]
            );
        } else loessDomain=null;
    }

    public double value(double x) {
        if (loessDomain==null || !loessDomain.contains(x) ) {
            return linearBackup.value(x);
        } else return loessFunction.value(x);
    }
}
