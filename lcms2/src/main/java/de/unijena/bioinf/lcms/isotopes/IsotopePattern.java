package de.unijena.bioinf.lcms.isotopes;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.ms.utils.BasicSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class IsotopePattern extends SimpleSpectrum {


    protected static final double MZ_ISO_ERRT = 0.002;
    protected static final Range<Double>[] ISO_RANGES = new Range[]{
            Range.closed(0.99664664 - MZ_ISO_ERRT, 1.00342764 + MZ_ISO_ERRT),
            Range.closed(1.99653883209004 - MZ_ISO_ERRT, 2.0067426280592295 + MZ_ISO_ERRT),
            Range.closed(2.9950584 - MZ_ISO_ERRT, 3.00995027 + MZ_ISO_ERRT),
            Range.closed(3.99359037 - MZ_ISO_ERRT, 4.01300058 + MZ_ISO_ERRT),
            Range.closed(4.9937908 - MZ_ISO_ERRT, 5.01572941 + MZ_ISO_ERRT)
    };

    public final static int MINIMUM_ION_SIZE = 100;

    public static Optional<IsotopePattern> extractPattern(SimpleSpectrum spectrum, int peakIdx) {
        List<IsotopePattern> patterns = new ArrayList<>();
        final DoubleArrayList mzs = new DoubleArrayList(), intensities = new DoubleArrayList();
        final double mz = spectrum.getMzAt(peakIdx);
        final double intens = spectrum.getIntensityAt(peakIdx);
        for (int chargeState=1; chargeState <= 3; ++chargeState) {
            if (chargeState > 1 && mz / chargeState < MINIMUM_ION_SIZE)
                continue; // we do not believe in too low-mass peaks with multiple charges
            mzs.clear(); mzs.add(mz);
            intensities.clear(); intensities.add(intens);
            forEachIsotopePeak:
            for (int k = 0; k < ISO_RANGES.length; ++k) {
                // try to detect +k isotope peak
                final double maxMz = mz + ISO_RANGES[k].upperEndpoint() / chargeState;
                double mergedIntensity = 0d;
                double mergedMass = 0d;
                final int a = Spectrums.indexOfFirstPeakWithin(spectrum, mz + ISO_RANGES[k].lowerEndpoint() / chargeState, maxMz);
                if (a < 0) break forEachIsotopePeak;
                for (int i = a; i < spectrum.size(); ++i) {
                    if (spectrum.getMzAt(i) > maxMz)
                        break;
                    mergedIntensity += spectrum.getIntensityAt(a);
                    mergedMass += spectrum.getIntensityAt(a) * spectrum.getMzAt(a);
                }
                mergedMass /= mergedIntensity;
                mzs.add(mergedMass);
                intensities.add(mergedIntensity);
            }
            if (mzs.size()>1)
                patterns.add(new IsotopePattern(mzs.toDoubleArray(), intensities.toDoubleArray(), chargeState));
        }
        return patterns.stream().filter(x->x.size()>1).max(Comparator.comparingInt(BasicSpectrum::size));
    }

    public final int chargeState;
    protected IsotopePattern(double[] masses, double[] intensities, int chargeState) {
        super(masses, normalized(intensities));
        this.chargeState = chargeState;
    }

    private static double[] normalized(double[] xs) {
        double sum = 0d;
        for (double x : xs) sum += x;
        for (int k=0; k < xs.length; ++k) xs[k]/=sum;
        return xs;
    }
}
