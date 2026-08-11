package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static de.unijena.bioinf.lcms.deconvolution.DiaPipelineFileTest.file;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Judges the deconvolution against a DDA measurement of the same sample, because the number of peaks a
 * deconvolution produces says nothing about whether they belong to the precursor they were assigned to. A peak
 * assigned to two precursors instead of one is only more information if the second precursor really produced it,
 * and the cheapest way to find out is to look up whether the DDA spectrum of that precursor contains the peak.
 * <p>
 * Three runs, so the two settings are compared on the same compounds rather than through aggregate counts: the DDA
 * file as reference, and the DIA file with the intensity prior on and off. Features are matched by precursor mass
 * and retention time, and only the overlap is scored - DDA sees fewer compounds, and a compound it never selected
 * for fragmentation cannot say anything about our assignment.
 * <p>
 * Every rate is reported next to a decoy rate, obtained by scoring the same peaks against the DDA spectrum of a
 * different compound of similar precursor mass that elutes far away. A fragment mass matches some peak of some
 * spectrum by chance surprisingly often, and without that control a confirmation rate cannot be read at all.
 * <p>
 * The decisive number is the last block: of the peaks the prior removes, how many were in the DDA spectrum. Those
 * are the ones it got wrong; the removed peaks that are absent from the DDA spectrum, it removed correctly.
 * <p>
 * Skipped unless both files are given:
 * <pre>
 * SIRIUS_DIA_TEST_FILE=/path/SWATH_1.mzML SIRIUS_DDA_TEST_FILE=/path/DDA_1.mzML \
 *   ./gradlew :lcms2:cleanTest :lcms2:test --tests '*DiaVersusDdaFileTest*'
 * </pre>
 */
public class DiaVersusDdaFileTest {

    /** for matching a fragment peak of one spectrum against the peaks of another */
    private static final Deviation PEAK_DEVIATION = new Deviation(10, 0.002);

    /** for matching a feature of one run against the features of another */
    private static final Deviation PRECURSOR_DEVIATION = new Deviation(10, 0.002);

    /** the same compound in two runs of the same sample on the same column, in seconds */
    private static final double DEFAULT_RT_TOLERANCE = 20d;

    /** a decoy has to be far enough away that it cannot be the same compound, in seconds */
    private static final double MIN_DECOY_RT_DISTANCE = 120d;

    /**
     * Peaks this close to the precursor are the surviving precursor ion and its isotopes. They are in every
     * spectrum of that precursor by construction, ours as much as the DDA one, so counting them would report
     * agreement that is not evidence of anything.
     */
    private static final double PRECURSOR_REGION = 1.5;

    /** below this share of the base peak a DDA peak is not reliably a fragment, so recall is not asked about it */
    private static final double MIN_RELATIVE_INTENSITY_FOR_RECALL = 0.01;

    @Test
    public void compareAgainstDda() throws IOException {
        final Optional<Path> diaFile = file("SIRIUS_DIA_TEST_FILE");
        final Optional<Path> ddaFile = file("SIRIUS_DDA_TEST_FILE");
        assumeTrue(diaFile.isPresent() && ddaFile.isPresent(),
                "no file pair given, set SIRIUS_DIA_TEST_FILE and SIRIUS_DDA_TEST_FILE");

        final double rtTolerance = doubleFromEnvironment("SIRIUS_MATCH_RT_TOLERANCE", DEFAULT_RT_TOLERANCE);
        final Double quantile = System.getenv("SIRIUS_DIA_NOISE_QUANTILE") == null ? null
                : Double.parseDouble(System.getenv("SIRIUS_DIA_NOISE_QUANTILE"));

        // only the spectra are kept from each run: three whole runs' features at once do not fit in a test JVM
        final Run reference = Run.of(ddaFile.get(), "DDA reference", null);
        final Run priorOn = Run.of(diaFile.get(), "DIA, prior on", deconvolution -> {
            deconvolution.setIntensityPrior(true);
            if (quantile != null) deconvolution.setNoiseQuantile(quantile);
        });
        final Run priorOff = Run.of(diaFile.get(), "DIA, prior off", deconvolution -> {
            deconvolution.setIntensityPrior(false);
            if (quantile != null) deconvolution.setNoiseQuantile(quantile);
        });

        System.out.printf("%n=== reference: %s ===%n", ddaFile.get().getFileName());
        System.out.printf("%d features, %d with a merged MS/MS%n", reference.featureCount, reference.spectra.size());

        final Agreement on = score(priorOn, reference, rtTolerance);
        final Agreement off = score(priorOff, reference, rtTolerance);
        System.out.printf("%n=== agreement with the reference (retention time tolerance %.0f s) ===%n", rtTolerance);
        System.out.printf("%-15s %8s %8s %8s %11s %10s %10s %8s%n",
                "", "features", "matched", "peaks", "confirmed", "by decoy", "weighted", "recall");
        System.out.println(on.describe());
        System.out.println(off.describe());

        final Removed removed = scoreRemovedPeaks(priorOff, priorOn, reference, rtTolerance);
        System.out.printf("%n=== the peaks the prior removes (%d compounds present in all three runs) ===%n",
                removed.features);
        System.out.println(removed.describe());

        assertTrue(!reference.spectra.isEmpty(), "the reference run must produce MS/MS");
        assertTrue(on.matchedFeatures > 0 && off.matchedFeatures > 0, "and features must match across runs");
        // if the assignment carried no information, a peak would agree with the reference no more often than with
        // an unrelated spectrum of similar precursor mass
        assertTrue(on.confirmedRate() > on.decoyRate(), "prior on: agreement must beat chance");
        assertTrue(off.confirmedRate() > off.decoyRate(), "prior off: agreement must beat chance");
    }

    private static double doubleFromEnvironment(String variable, double fallback) {
        final String value = System.getenv(variable);
        return value == null || value.isBlank() ? fallback : Double.parseDouble(value);
    }

    /** how well one run's fragment peaks agree with the reference run's spectra of the same compounds */
    private static Agreement score(Run run, Run reference, double rtTolerance) {
        final Agreement agreement = new Agreement(run.name, run.featureCount);
        for (FeatureSpectrum ours : run.spectra) {
            final FeatureSpectrum match = reference.bestMatch(ours, rtTolerance);
            if (match == null) continue;
            final FeatureSpectrum decoy = reference.decoy(ours);
            ++agreement.matchedFeatures;

            for (int k = 0; k < ours.size(); ++k) {
                final double mz = ours.mz[k];
                final double intensity = ours.intensity[k];
                ++agreement.peaks;
                agreement.intensity += intensity;
                if (match.contains(mz)) {
                    ++agreement.confirmed;
                    agreement.confirmedIntensity += intensity;
                }
                if (decoy != null) {
                    ++agreement.decoyPeaks;
                    if (decoy.contains(mz)) ++agreement.decoyConfirmed;
                }
            }
            // and the other direction: which of the reference fragments did we find at all
            for (int k = 0; k < match.size(); ++k) {
                if (match.intensity[k] < MIN_RELATIVE_INTENSITY_FOR_RECALL * match.basePeakIntensity) continue;
                ++agreement.referencePeaks;
                if (ours.contains(match.mz[k])) ++agreement.recovered;
            }
        }
        return agreement;
    }

    /**
     * The comparison aggregate counts cannot make: take the compounds all three runs found, split the peaks the
     * run without the prior produced into those the prior kept and those it removed, and ask the reference about
     * each group separately.
     */
    private static Removed scoreRemovedPeaks(Run priorOff, Run priorOn, Run reference, double rtTolerance) {
        final Removed result = new Removed();
        for (FeatureSpectrum ours : priorOff.spectra) {
            final FeatureSpectrum counterpart = priorOn.bestMatch(ours, rtTolerance);
            if (counterpart == null) continue;
            final FeatureSpectrum match = reference.bestMatch(ours, rtTolerance);
            if (match == null) continue;
            final FeatureSpectrum decoy = reference.decoy(ours);
            ++result.features;

            for (int k = 0; k < ours.size(); ++k) {
                final double mz = ours.mz[k];
                final Group group = counterpart.contains(mz) ? result.kept : result.removed;
                ++group.peaks;
                group.intensity += ours.intensity[k];
                if (match.contains(mz)) {
                    ++group.confirmed;
                    group.confirmedIntensity += ours.intensity[k];
                }
                if (decoy != null) {
                    ++group.decoyPeaks;
                    if (decoy.contains(mz)) ++group.decoyConfirmed;
                }
            }
        }
        return result;
    }

    /**
     * The fragment peaks of one feature: everything the comparison needs, and nothing else, so that three runs fit
     * in memory at once. Masses are ascending, and peaks in the precursor region are already gone.
     */
    private static final class FeatureSpectrum {
        private final double precursorMz;
        private final double retentionTime;
        private final double[] mz;
        private final double[] intensity;
        private final double basePeakIntensity;

        private FeatureSpectrum(double precursorMz, double retentionTime, double[] mz, double[] intensity) {
            this.precursorMz = precursorMz;
            this.retentionTime = retentionTime;
            this.mz = mz;
            this.intensity = intensity;
            double base = 0d;
            for (double i : intensity) base = Math.max(base, i);
            this.basePeakIntensity = base;
        }

        static FeatureSpectrum of(AlignedFeatures feature) {
            final SimpleSpectrum spectrum = feature.getMSData().orElseThrow().getMergedMSnSpectrum();
            final double precursorMz = feature.getAverageMass();
            final DoubleArrayList masses = new DoubleArrayList(spectrum.size());
            final DoubleArrayList intensities = new DoubleArrayList(spectrum.size());
            for (int k = 0; k < spectrum.size(); ++k) {
                if (spectrum.getMzAt(k) > precursorMz - PRECURSOR_REGION) continue;
                masses.add(spectrum.getMzAt(k));
                intensities.add(spectrum.getIntensityAt(k));
            }
            return new FeatureSpectrum(precursorMz, feature.getRetentionTime().getRetentionTimeInSeconds(),
                    masses.toDoubleArray(), intensities.toDoubleArray());
        }

        int size() {
            return mz.length;
        }

        /** whether some peak of this spectrum has the given mass, within {@link #PEAK_DEVIATION} */
        boolean contains(double queryMz) {
            final double allowed = PEAK_DEVIATION.absoluteFor(queryMz);
            int low = 0, high = mz.length - 1;
            while (low <= high) {
                final int mid = (low + high) >>> 1;
                if (mz[mid] < queryMz - allowed) low = mid + 1;
                else if (mz[mid] > queryMz + allowed) high = mid - 1;
                else return true;
            }
            return false;
        }
    }

    /** the spectra of one run, searchable by precursor mass and retention time */
    private static final class Run {
        private final String name;
        private final int featureCount;
        private final List<FeatureSpectrum> spectra;

        private Run(String name, int featureCount, List<FeatureSpectrum> spectra) {
            this.name = name;
            this.featureCount = featureCount;
            this.spectra = spectra;
            this.spectra.sort(Comparator.comparingDouble(s -> s.precursorMz));
        }

        /**
         * Runs the file and keeps only the spectra. The features themselves are unreachable when this returns,
         * which is what makes three runs in one test JVM possible.
         */
        static Run of(Path file, String name, Consumer<DiaDeconvolution> configure) throws IOException {
            final PipelineRun run = PipelineRun.run(file, name, configure);
            final List<FeatureSpectrum> spectra = new ArrayList<>();
            for (AlignedFeatures feature : run.featuresWithMsMs()) {
                final FeatureSpectrum spectrum = FeatureSpectrum.of(feature);
                if (spectrum.size() > 0) spectra.add(spectrum);
            }
            System.out.printf("%s: %d features, %d with fragment peaks, %.1f s%n", name, run.features.size(),
                    spectra.size(), run.processingSeconds + run.alignmentSeconds);
            return new Run(name, run.features.size(), spectra);
        }

        /** the mass matching spectrum closest in retention time, or null if none is within the tolerance */
        FeatureSpectrum bestMatch(FeatureSpectrum query, double rtTolerance) {
            final double allowed = PRECURSOR_DEVIATION.absoluteFor(query.precursorMz);
            FeatureSpectrum best = null;
            double bestDistance = Double.POSITIVE_INFINITY;
            for (int k = lowerBound(query.precursorMz - allowed);
                 k < spectra.size() && spectra.get(k).precursorMz <= query.precursorMz + allowed; ++k) {
                final double distance = Math.abs(spectra.get(k).retentionTime - query.retentionTime);
                if (distance <= rtTolerance && distance < bestDistance) {
                    bestDistance = distance;
                    best = spectra.get(k);
                }
            }
            return best;
        }

        /**
         * A spectrum that cannot be the queried compound but covers a comparable mass range: the closest precursor
         * mass among the compounds eluting far away. Scoring against it measures how often a fragment mass finds a
         * peak by chance.
         */
        FeatureSpectrum decoy(FeatureSpectrum query) {
            // walking outwards from the query mass finds the closest eligible precursor without scanning the rest
            int left = lowerBound(query.precursorMz) - 1, right = left + 1;
            while (left >= 0 || right < spectra.size()) {
                final boolean takeLeft = right >= spectra.size() || (left >= 0
                        && query.precursorMz - spectra.get(left).precursorMz
                        <= spectra.get(right).precursorMz - query.precursorMz);
                final FeatureSpectrum candidate = takeLeft ? spectra.get(left--) : spectra.get(right++);
                if (Math.abs(candidate.retentionTime - query.retentionTime) >= MIN_DECOY_RT_DISTANCE) {
                    return candidate;
                }
            }
            return null;
        }

        /** index of the first spectrum whose precursor mass is not below the given one */
        private int lowerBound(double precursorMz) {
            int low = 0, high = spectra.size();
            while (low < high) {
                final int mid = (low + high) >>> 1;
                if (spectra.get(mid).precursorMz < precursorMz) low = mid + 1;
                else high = mid;
            }
            return low;
        }
    }

    private static final class Agreement {
        private final String name;
        private final int features;
        private int matchedFeatures, peaks, confirmed, decoyPeaks, decoyConfirmed, referencePeaks, recovered;
        private double intensity, confirmedIntensity;

        Agreement(String name, int features) {
            this.name = name;
            this.features = features;
        }

        double confirmedRate() {
            return peaks == 0 ? 0d : confirmed / (double) peaks;
        }

        double decoyRate() {
            return decoyPeaks == 0 ? 0d : decoyConfirmed / (double) decoyPeaks;
        }

        String describe() {
            return String.format("%-15s %8d %8d %8d %9.1f %% %8.1f %% %8.1f %% %6.1f %%", name, features,
                    matchedFeatures, peaks, 100 * confirmedRate(), 100 * decoyRate(),
                    intensity == 0 ? 0d : 100 * confirmedIntensity / intensity,
                    referencePeaks == 0 ? 0d : 100 * recovered / (double) referencePeaks);
        }
    }

    private static final class Group {
        private int peaks, confirmed, decoyPeaks, decoyConfirmed;
        private double intensity, confirmedIntensity;

        String describe(String label) {
            return String.format("%-22s %7d peaks, %5.1f %% in the reference (%5.1f %% by decoy), "
                            + "%5.1f %% of their intensity", label, peaks,
                    peaks == 0 ? 0d : 100 * confirmed / (double) peaks,
                    decoyPeaks == 0 ? 0d : 100 * decoyConfirmed / (double) decoyPeaks,
                    intensity == 0 ? 0d : 100 * confirmedIntensity / intensity);
        }
    }

    private static final class Removed {
        private int features;
        private final Group kept = new Group();
        private final Group removed = new Group();

        String describe() {
            return kept.describe("kept by the prior") + "\n" + removed.describe("removed by the prior");
        }
    }
}
