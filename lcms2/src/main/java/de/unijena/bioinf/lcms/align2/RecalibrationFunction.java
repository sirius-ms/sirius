package de.unijena.bioinf.lcms.align2;

import com.google.common.collect.Range;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import javax.annotation.Nullable;

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
            loessDomain = Range.closed(
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
