package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.LCMSProcessing;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.statistics.TraceCorrelationStatistics;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.Trace;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Why a sample's known same-elution statistics come out unusable, told apart from the two things it could be.
 * <p>
 * {@link TraceCorrelationStatistics} bins its isotope pairs by the weaker partner's intensity <em>in multiples of the
 * sample's noise level</em>, and discards a bin whose correlations are too poor to read. When every bin is discarded
 * there are two quite different explanations and the report itself cannot separate them: either the isotope traces
 * really are that noisy, or the noise level the multiples are measured against is too high, in which case pairs that
 * are perfectly measurable are all filed under "near the noise" and the strong regime is never populated.
 * <p>
 * So this reports the same pairs twice - once against the noise level, once in absolute intensity - next to the
 * intensity distribution of the sample as a whole. If correlation is good at high absolute intensity while every
 * noise-relative bin is poor, the noise level is the fault. If correlation is poor even at the top of the sample's
 * own intensity range, the traces are.
 * <p>
 * A file test, skipped unless a file is given:
 * <pre>
 * SIRIUS_DIA_TEST_FILE=/path/sample.mzML \
 *   ./gradlew :lcms2:cleanTest :lcms2:test --tests '*IsotopePairIntensityFileTest*' -q
 * </pre>
 */
public class IsotopePairIntensityFileTest {

    /** lower bounds of the absolute intensity bands, chosen to straddle any plausible noise level */
    private static final double[] ABSOLUTE_BANDS = {0, 10, 30, 100, 300, 1_000, 3_000, 10_000, 30_000};

    private record Pair(double monoApex, double weakerApex, double correlation, int points) {
    }

    @Test
    public void isotopePairsAgainstNoiseAndAgainstAbsoluteIntensity() throws IOException {
        final List<Path> files = new ArrayList<>();
        for (String part : System.getenv().getOrDefault("SIRIUS_DIA_TEST_FILE", "").split(",")) {
            if (!part.isBlank() && Files.exists(Path.of(part.trim()))) files.add(Path.of(part.trim()));
        }
        assumeTrue(!files.isEmpty(), "no file given, set SIRIUS_DIA_TEST_FILE");

        final StringBuilder out = new StringBuilder();
        for (Path file : files.subList(0, Math.min(files.size(), 2))) {
            final LCMSProcessing processing = new LCMSProcessing(new de.unijena.bioinf.lcms.projectspace.CollectingDatabaseAdapter(), false);
            final ProcessedSample sample = processing.processSample(file);
            final SampleStats stats = sample.getStorage().getStatistics();
            final double noiseLevel = TraceCorrelationStatistics.medianNoiseLevel(stats);

            final List<Pair> pairs = collect(sample, new Deviation(10));
            out.append(String.format(Locale.US, "%n=== %s ===%n", file.getFileName()));
            out.append(String.format(Locale.US,
                    "noise level: median per scan %.1f, MS2 %.1f, minimum intensity %.1f%n",
                    noiseLevel, stats.getMs2NoiseLevel(), stats.getMinimumIntensity()));
            out.append(traceIntensityProfile(sample));
            // what the pipeline itself will log, so the effect of the floor is read off the same report
            out.append(stats.getTraceCorrelations().describe());
            out.append(String.format(Locale.US, "%n%d isotope pairs%n", pairs.size()));
            out.append(byNoiseMultiple(pairs, noiseLevel));
            out.append(byAbsoluteIntensity(pairs));
            processing.closeStorages();
        }
        System.out.println(out);
    }

    /** the sample's own intensity range, so "high absolute intensity" can be read as high for this sample */
    private static String traceIntensityProfile(ProcessedSample sample) {
        final DoubleArrayList apexes = new DoubleArrayList();
        for (ContiguousTrace trace : sample.getStorage().getTraceStorage()) apexes.add(trace.apexIntensity());
        final double[] sorted = apexes.toDoubleArray();
        java.util.Arrays.sort(sorted);
        return String.format(Locale.US,
                "%d traces, apex intensity: median %.0f, 90%% %.0f, 99%% %.0f, 99.9%% %.0f, max %.0f%n",
                sorted.length, quantile(sorted, 0.5), quantile(sorted, 0.9), quantile(sorted, 0.99),
                quantile(sorted, 0.999), sorted.length == 0 ? Double.NaN : sorted[sorted.length - 1]);
    }

    /** the same pair finding {@link TraceCorrelationStatistics#learn} does, at full resolution only */
    private static List<Pair> collect(ProcessedSample sample, Deviation deviation) {
        final List<Pair> pairs = new ArrayList<>();
        for (ContiguousTrace trace : sample.getStorage().getTraceStorage()) {
            final double mz = trace.averagedMz() + TraceCorrelationStatistics.CARBON_ISOTOPE_DELTA;
            final double allowed = deviation.absoluteFor(mz);
            for (ContiguousTrace isotope : sample.getStorage().getTraceStorage()
                    .getContigousTraces(mz - allowed, mz + allowed, trace.startId(), trace.endId())) {
                if (isotope.apexIntensity() >= trace.apexIntensity()) continue;
                final int from = Math.max(trace.startId(), isotope.startId());
                final int to = Math.min(trace.endId(), isotope.endId());
                if (to - from + 1 < 3) continue;
                final double[] left = profile(trace, from, to);
                final double[] right = profile(isotope, from, to);
                final double correlation = Statistics.pearson(left, right);
                if (!Double.isFinite(correlation)) continue;
                pairs.add(new Pair(apexWithin(trace, from, to), Math.min(apexWithin(trace, from, to),
                        apexWithin(isotope, from, to)), correlation, left.length));
            }
        }
        return pairs;
    }

    private static String byNoiseMultiple(List<Pair> pairs, double noiseLevel) {
        final StringBuilder buf = new StringBuilder(String.format(Locale.US,
                "%nweaker partner in multiples of the noise level (%.1f)%n", noiseLevel));
        final double[] bounds = TraceCorrelationStatistics.INTENSITY_MULTIPLES;
        for (int k = 0; k <= bounds.length; ++k) {
            final double low = k == 0 ? 0 : bounds[k - 1] * noiseLevel;
            final double high = k == bounds.length ? Double.MAX_VALUE : bounds[k] * noiseLevel;
            buf.append(band(pairs, k == 0 ? "below 1x" : (k == bounds.length ? "above 100x"
                    : String.format(Locale.US, "%.0f-%.0fx", bounds[k - 1], bounds[k])), low, high));
        }
        return buf.toString();
    }

    private static String byAbsoluteIntensity(List<Pair> pairs) {
        final StringBuilder buf = new StringBuilder("\nweaker partner in absolute intensity\n");
        for (int k = 0; k < ABSOLUTE_BANDS.length; ++k) {
            final double high = k + 1 < ABSOLUTE_BANDS.length ? ABSOLUTE_BANDS[k + 1] : Double.MAX_VALUE;
            buf.append(band(pairs, String.format(Locale.US, "%.0f-%s", ABSOLUTE_BANDS[k],
                    high == Double.MAX_VALUE ? "" : String.format(Locale.US, "%.0f", high)),
                    ABSOLUTE_BANDS[k], high));
        }
        return buf.toString();
    }

    /** one band, reported by the quantile the statistics judge a bin by so the numbers are comparable to it */
    private static String band(List<Pair> pairs, String label, double low, double high) {
        final DoubleArrayList values = new DoubleArrayList();
        for (Pair pair : pairs) {
            if (pair.weakerApex() >= low && pair.weakerApex() < high) values.add(pair.correlation());
        }
        if (values.isEmpty()) return String.format(Locale.US, "  %-12s        0 pairs%n", label);
        final double[] sorted = values.toDoubleArray();
        java.util.Arrays.sort(sorted);
        return String.format(Locale.US, "  %-12s %8d pairs   5%% %6.3f   median %6.3f   mean %6.3f%n",
                label, sorted.length, quantile(sorted, 0.05), quantile(sorted, 0.5), mean(sorted));
    }

    private static double quantile(double[] sorted, double q) {
        if (sorted.length == 0) return Double.NaN;
        return sorted[Math.min(sorted.length - 1, Math.max(0, (int) Math.floor(q * sorted.length)))];
    }

    private static double mean(double[] values) {
        double sum = 0d;
        for (double value : values) sum += value;
        return sum / values.length;
    }

    private static double[] profile(Trace trace, int from, int to) {
        final double[] profile = new double[to - from + 1];
        for (int k = 0; k < profile.length; ++k) profile[k] = trace.intensity(from + k);
        return profile;
    }

    private static double apexWithin(Trace trace, int from, int to) {
        double apex = 0d;
        for (int k = from; k <= to; ++k) apex = Math.max(apex, trace.intensity(k));
        return apex;
    }
}
