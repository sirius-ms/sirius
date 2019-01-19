package de.unijena.bioinf.sirius.annotations;

import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.RecalibrationFunction;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.sirius.MS2Peak;
import de.unijena.bioinf.sirius.ProcessedPeak;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;

public class SpectralRecalibration implements DataAnnotation {

    private final static SpectralRecalibration NONE = new SpectralRecalibration(null,null,null);

    public static SpectralRecalibration none() {
        return NONE;
    }

    protected final UnivariateFunction[] recalibrationFunctions;
    protected final UnivariateFunction mergedFunc;

    public SpectralRecalibration(MutableMs2Spectrum[] originalSpectra, UnivariateFunction[] recalibrationFunctions, UnivariateFunction mergedFunc) {
        this.recalibrationFunctions = recalibrationFunctions;
        this.mergedFunc = mergedFunc==null ? new Identity() : mergedFunc;
    }

    public UnivariateFunction getRecalibrationFunction() {
        return mergedFunc;
    }

    public UnivariateFunction getRecalibrationFunctionFor(MutableMs2Spectrum spec) {
        if (recalibrationFunctions==null) return mergedFunc;
        final UnivariateFunction f = recalibrationFunctions[spec.getScanNumber()];
        if (f==null) return mergedFunc;
        else return f;
    }

    public double recalibrate(ProcessedPeak peak) {
        if (this==NONE) return peak.getMass();
        // 1. check if most intensive original peak can be recalibrated
        MS2Peak mostIntensive = null;
        for (MS2Peak m : peak.getOriginalPeaks()) {
            if (mostIntensive==null || m.getIntensity() > mostIntensive.getIntensity())
                mostIntensive = m;
        }
        if (mostIntensive!=null) {
            final int sc = ((MutableMs2Spectrum)mostIntensive.getSpectrum()).getScanNumber();
            if (recalibrationFunctions[sc]!=null) {
                return recalibrationFunctions[sc].value(peak.getMass());
            }
        }
        // 2. use merged recalibration function
        return mergedFunc.value(peak.getMass());
    }


    public RecalibrationFunction toPolynomial() {
        final UnivariateFunction f = getRecalibrationFunction();
        if (f instanceof PolynomialFunction)
            return new RecalibrationFunction(((PolynomialFunction) f).getCoefficients());
        else if (f instanceof Identity)
            return RecalibrationFunction.identity();
        else throw new RuntimeException("Cannot represent " + f + " as polynomial function");
    }
}
