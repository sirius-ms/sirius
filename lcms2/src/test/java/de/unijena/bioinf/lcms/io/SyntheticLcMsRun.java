package de.unijena.bioinf.lcms.io;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A small simulated LC/MS measurement, written as mzML, so that the whole preprocessing can be run on input whose
 * answer is known: which compounds elute when, and which fragmentation spectrum belongs to which of them.
 * <p>
 * The point of simulating rather than shipping a real file is the MS/MS only run - the case this exists for - which
 * has to be paired with an ordinary run of the same compounds to be worth anything. The simulation is deliberately
 * plain: gaussian elution profiles, a natural carbon isotope pattern, and a uniform background so that the noise
 * estimation has something to estimate.
 */
public final class SyntheticLcMsRun {

    /** the mass difference between two carbon isotopologues */
    private static final double C13_DELTA = 1.0033548;
    /** natural abundance of 13C */
    private static final double C13_ABUNDANCE = 0.0107;

    /** distance between two scan points, in seconds. Gives three scan points per sigma, as a real chromatogram does */
    public static final double SCAN_INTERVAL = 2d;
    public static final double RUN_LENGTH = 400d;
    /** standard deviation of the elution profile, in seconds */
    public static final double PEAK_SIGMA = 6d;

    private static final int BACKGROUND_PEAKS_PER_SCAN = 60;
    private static final double BACKGROUND_MAX_INTENSITY = 30d;

    /**
     * @param precursorMz       monoisotopic m/z of the precursor ion
     * @param apexRetentionTime where it elutes, in seconds
     * @param apexIntensity     height of the elution profile at its apex
     * @param carbonCount       how many carbons the molecule has, which is what the isotope pattern is made of
     * @param fragmentMz        the fragment masses of its MS/MS
     * @param fragmentIntensity the intensities belonging to {@code fragmentMz}
     */
    public record Compound(double precursorMz, double apexRetentionTime, double apexIntensity, int carbonCount,
                    double[] fragmentMz, double[] fragmentIntensity) {

        double intensityAt(double retentionTime) {
            final double z = (retentionTime - apexRetentionTime) / PEAK_SIGMA;
            return apexIntensity * Math.exp(-0.5 * z * z);
        }
    }

    private SyntheticLcMsRun() {
    }

    /**
     * An ordinary data dependent run: MS1 scans over the whole gradient, and one MS/MS per compound taken at the scan
     * point closest to its apex.
     *
     * @param seed fixes the background, so that a test is reproducible and two runs can be made to differ
     */
    public static Path writeLcMsRun(Path file, List<Compound> compounds, long seed) throws IOException {
        final SyntheticMzML mzml = SyntheticMzML.builder();
        final Random random = new Random(seed);
        for (double rt = 0; rt <= RUN_LENGTH + 1e-9; rt += SCAN_INTERVAL) {
            final int scanNumber = writeMs1Scan(mzml, random, compounds, rt);
            for (Compound compound : compounds) {
                if (Math.abs(compound.apexRetentionTime() - rt) < SCAN_INTERVAL / 2) {
                    mzml.ms2(rt + SCAN_INTERVAL / 4, compound.precursorMz(), 1d, 20d, scanNumber,
                            compound.fragmentMz(), compound.fragmentIntensity());
                }
            }
        }
        return mzml.writeTo(file);
    }

    /**
     * A run holding nothing but the fragmentation spectra, at the retention times the compounds elute at. This is
     * what an instrument produces when the survey scans went into a separate file, or when an export kept only the
     * MS/MS.
     */
    public static Path writeMs2OnlyRun(Path file, List<Compound> compounds) throws IOException {
        final SyntheticMzML mzml = SyntheticMzML.builder();
        final List<Compound> byRetentionTime = new ArrayList<>(compounds);
        byRetentionTime.sort((a, b) -> Double.compare(a.apexRetentionTime(), b.apexRetentionTime()));
        for (Compound compound : byRetentionTime) {
            mzml.ms2(compound.apexRetentionTime(), compound.precursorMz(), 1d, 20d, null,
                    compound.fragmentMz(), compound.fragmentIntensity());
        }
        return mzml.writeTo(file);
    }

    private static int writeMs1Scan(SyntheticMzML mzml, Random random, List<Compound> compounds, double rt) {
        final DoubleArrayList mzValues = new DoubleArrayList();
        final DoubleArrayList intensities = new DoubleArrayList();
        for (int k = 0; k < BACKGROUND_PEAKS_PER_SCAN; ++k) {
            mzValues.add(50d + random.nextDouble() * 550d);
            intensities.add(1d + random.nextDouble() * BACKGROUND_MAX_INTENSITY);
        }
        for (Compound compound : compounds) {
            final double intensity = compound.intensityAt(rt);
            if (intensity < BACKGROUND_MAX_INTENSITY) continue;
            final double[] isotopes = isotopeAbundances(compound.carbonCount());
            for (int k = 0; k < isotopes.length; ++k) {
                final double peakIntensity = intensity * isotopes[k];
                if (peakIntensity < 1d) break;
                mzValues.add(compound.precursorMz() + k * C13_DELTA);
                intensities.add(peakIntensity);
            }
        }
        sortByMz(mzValues, intensities);
        return mzml.ms1(rt, mzValues.toDoubleArray(), intensities.toDoubleArray());
    }

    /** monoisotopic, +1 and +2 peak of a molecule with the given number of carbons, relative to the monoisotopic one */
    private static double[] isotopeAbundances(int carbonCount) {
        final double plusOne = carbonCount * C13_ABUNDANCE;
        final double plusTwo = carbonCount * (carbonCount - 1) / 2d * C13_ABUNDANCE * C13_ABUNDANCE;
        return new double[]{1d, plusOne, plusTwo};
    }

    private static void sortByMz(DoubleArrayList mzValues, DoubleArrayList intensities) {
        final Integer[] order = new Integer[mzValues.size()];
        for (int k = 0; k < order.length; ++k) order[k] = k;
        java.util.Arrays.sort(order, (a, b) -> Double.compare(mzValues.getDouble(a), mzValues.getDouble(b)));
        final double[] sortedMz = new double[order.length], sortedIntensity = new double[order.length];
        for (int k = 0; k < order.length; ++k) {
            sortedMz[k] = mzValues.getDouble(order[k]);
            sortedIntensity[k] = intensities.getDouble(order[k]);
        }
        mzValues.clear();
        intensities.clear();
        mzValues.addElements(0, sortedMz);
        intensities.addElements(0, sortedIntensity);
    }
}
