package de.unijena.bioinf.lcms.align;

import com.google.common.collect.Range;
import de.unijena.bioinf.lcms.LoessFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

public class RecalibrationFunction implements UnivariateFunction {

    private final Range<Double> loessDomain;
    private final PolynomialSplineFunction loessFunction;
    private final PolynomialFunction linearBackup;

    public static RecalibrationFunction fromLoess(PolynomialSplineFunction loessFunction, UnivariateFunction linearBackup) {
        if (linearBackup instanceof Identity) linearBackup=null;
        if (linearBackup!=null && !(linearBackup instanceof PolynomialFunction))
            throw new IllegalArgumentException("backup function should be a polynomial");
        if (loessFunction != null) {
            double[] knots = loessFunction.getKnots();
            return new RecalibrationFunction(Range.closed(knots[0], knots[knots.length-1]),
                loessFunction, (PolynomialFunction)linearBackup);
        } else return new RecalibrationFunction(Range.open(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
                null, (PolynomialFunction)linearBackup);
    }

    public RecalibrationFunction(Range<Double> loessDomain, PolynomialSplineFunction loessFunction, PolynomialFunction linearBackup) {
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
