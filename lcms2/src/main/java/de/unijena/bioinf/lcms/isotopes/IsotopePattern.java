package de.unijena.bioinf.lcms.isotopes;

import org.apache.commons.lang3.Range;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.ms.utils.BasicSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import java.util.*;

public class IsotopePattern extends SimpleSpectrum {


    protected static final double MZ_ISO_ERRT = 0.002;
    protected static final Range<Double>[] ISO_RANGES = new Range[]{
            Range.of(0.99664664 - MZ_ISO_ERRT, 1.00342764 + MZ_ISO_ERRT),
            Range.of(1.99653883209004 - MZ_ISO_ERRT, 2.0067426280592295 + MZ_ISO_ERRT),
            Range.of(2.9950584 - MZ_ISO_ERRT, 3.00995027 + MZ_ISO_ERRT),
            Range.of(3.99359037 - MZ_ISO_ERRT, 4.01300058 + MZ_ISO_ERRT),
            Range.of(4.9937908 - MZ_ISO_ERRT, 5.01572941 + MZ_ISO_ERRT)
    };

    protected static double maxStd = 0.0027340053758699856;
    protected static double avg = 1.00069363462413;

    public final static int MINIMUM_ION_SIZE = 100;

    /**
     * returns the minimal m/z for an isotope peak
     * @param ionMass ion mass of the monoisotopic peak
     * @param isotopePeak 0 for monoisotopic, 1 for first isotope and so on
     * @param chargeState
     * @return
     */
    public static double getMinimumMzFor(double ionMass, int isotopePeak, int chargeState) {
        if (isotopePeak > ISO_RANGES.length) {
            return (avg*isotopePeak - maxStd) / chargeState;
        } else {
            return ionMass + (ISO_RANGES[isotopePeak-1].getMinimum() / chargeState);
        }
    }

    /**
     * returns the maximal m/z for an isotope peak
     * @param ionMass ion mass of the monoisotopic peak
     * @param isotopePeak 0 for monoisotopic, 1 for first isotope and so on
     * @param chargeState
     * @return
     */
    public static double getMaximumMzFor(double ionMass, int isotopePeak, int chargeState) {
        if (isotopePeak > ISO_RANGES.length) {
            return (avg*isotopePeak + maxStd) / chargeState;
        } else {
            return ionMass + (ISO_RANGES[isotopePeak-1].getMaximum() / chargeState);
        }
    }

    public static boolean isMonoisotopic(SimpleSpectrum spectrum, int peakIdx) {
        return getPatternsForNonMonoisotopicPeak(spectrum,peakIdx).isEmpty();
    }

    public static List<IsotopePattern> getPatternsForNonMonoisotopicPeak(SimpleSpectrum spectrum, int peakIdx) {
        List<IsotopePattern> patterns = new ArrayList<>();
        final DoubleArrayList mzs = new DoubleArrayList(), intensities = new DoubleArrayList();
        final double mz = spectrum.getMzAt(peakIdx);
        final double intens = spectrum.getIntensityAt(peakIdx);
        for (int chargeState=1; chargeState <= 3; ++chargeState) {
            if (chargeState > 1 && mz / chargeState < MINIMUM_ION_SIZE)
                continue; // we do not believe in too low-mass peaks with multiple charges
            mzs.clear();
            mzs.add(mz);
            intensities.clear();
            intensities.add(intens);
            forEachIsotopePeak:
            for (int k = 0; k < ISO_RANGES.length; ++k) {
                // do not use ISO_RANGES directly, but just maximum range, as we do not know the start point here
                final double maxMz = mz - ((avg - maxStd) * (k + 1)) / chargeState;
                final double minMz = mz - ((avg + maxStd) * (k + 1)) / chargeState;
                double mergedIntensity = 0d;
                double mergedMass = 0d;
                final int a = Spectrums.indexOfFirstPeakWithin(spectrum, minMz, maxMz);
                if (a < 0) break forEachIsotopePeak;
                for (int i = a; i < spectrum.size(); ++i) {
                    if (spectrum.getMzAt(i) > maxMz)
                        break;
                    mergedIntensity += spectrum.getIntensityAt(i);
                    mergedMass += spectrum.getIntensityAt(i) * spectrum.getMzAt(i);
                }
                mergedMass /= mergedIntensity;
                mzs.add(mergedMass);
                intensities.add(mergedIntensity);
            }
            if (mzs.size() > 1) {
                revert(mzs);
                revert(intensities);
                if (intensities.doubleStream().sum() > intens * 2) {
                    // expand pattern the other side
                    expandPattern(spectrum, mzs, intensities, chargeState);
                    patterns.add(new IsotopePattern(mzs.toDoubleArray(), intensities.toDoubleArray(), chargeState));
                }
            }
        }
        return patterns;
    }

    private static void expandPattern(SimpleSpectrum spectrum, DoubleArrayList mzs, DoubleArrayList intensities, int chargeState) {
        final double mz = mzs.getDouble(0);
        forEachIsotopePeak:
        for (int k = mzs.size()-1; k < ISO_RANGES.length; ++k) {
            // try to detect +k isotope peak
            final double maxMz = mz + ISO_RANGES[k].getMaximum() / chargeState;
            double mergedIntensity = 0d;
            double mergedMass = 0d;
            final int a = Spectrums.indexOfFirstPeakWithin(spectrum, mz + ISO_RANGES[k].getMinimum() / chargeState, maxMz);
            if (a < 0) break forEachIsotopePeak;
            for (int i = a; i < spectrum.size(); ++i) {
                if (spectrum.getMzAt(i) > maxMz)
                    break;
                mergedIntensity += spectrum.getIntensityAt(i);
                mergedMass += spectrum.getIntensityAt(i) * spectrum.getMzAt(i);
            }
            mergedMass /= mergedIntensity;
            mzs.add(mergedMass);
            intensities.add(mergedIntensity);
        }
    }

    private static void revert(DoubleArrayList xs) {
        for (int k=0, n= xs.size()>>1; k < n; ++k) {
            int mir = xs.size()-(k+1);
            final double mem = xs.getDouble(k);
            xs.set(k, xs.getDouble(mir));
            xs.set(mir, mem);
        }
    }

    public static Optional<IsotopePattern> extractPattern(SimpleSpectrum spectrum, int peakIdx) {
        List<IsotopePattern> isotopePatterns = extractPatterns(spectrum, peakIdx);
        if (isotopePatterns.isEmpty()) return Optional.empty();
        else return Optional.of(isotopePatterns.get(0));
    }

    public static List<IsotopePattern> extractPatterns(SimpleSpectrum spectrum, int peakIdx) {
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
                final double maxMz = mz + ISO_RANGES[k].getMaximum() / chargeState;
                double mergedIntensity = 0d;
                double mergedMass = 0d;
                final int a = Spectrums.indexOfFirstPeakWithin(spectrum, mz + ISO_RANGES[k].getMinimum() / chargeState, maxMz);
                if (a < 0) break forEachIsotopePeak;
                for (int i = a; i < spectrum.size(); ++i) {
                    if (spectrum.getMzAt(i) > maxMz)
                        break;
                    mergedIntensity += spectrum.getIntensityAt(i);
                    mergedMass += spectrum.getIntensityAt(i) * spectrum.getMzAt(i);
                }
                mergedMass /= mergedIntensity;
                mzs.add(mergedMass);
                intensities.add(mergedIntensity);
            }
            if (mzs.size()>1)
                patterns.add(new IsotopePattern(mzs.toDoubleArray(), intensities.toDoubleArray(), chargeState));
        }
        return patterns.stream().filter(x->x.size()>1).sorted(Comparator.comparingInt(BasicSpectrum::size)).toList();
    }

    public final int chargeState;
    public IsotopePattern(double[] masses, double[] intensities, int chargeState) {
        super(masses, normalized(intensities));
        this.chargeState = chargeState;
    }

    private static double[] normalized(double[] xs) {
        double sum = 0d;
        for (double x : xs) sum += x;
        for (int k=0; k < xs.length; ++k) xs[k]/=sum;
        return xs;
    }

    public float[] floatIntensityArray() {
        return MatrixUtils.double2float(intensities);
    }

    public float[] floatMzArray() {
        final float[] mz = new float[masses.length];
        for (int i=0; i < masses.length; ++i) {
            mz[i] = (float)masses[i];
        }
        return mz;
    }
}
