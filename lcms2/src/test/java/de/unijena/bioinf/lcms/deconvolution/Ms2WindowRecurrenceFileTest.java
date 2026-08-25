package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.lcms.LCMSProcessing;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * How often each isolation window recurs and how densely, which is what decides whether its scans can be
 * deconvoluted at all.
 * <p>
 * Deconvolution correlates elution profiles, so a window is usable only if it is revisited often enough that
 * consecutive visits sample the same chromatographic peak several times. That is a property of one window rather
 * than of the file: an acquisition may hold windows of both kinds, and the same width can occur in both.
 * <p>
 * Reported per window so that a per-window rule can be calibrated against real files of both kinds - a genuine
 * SWATH run, and a data dependent run that merely repeats some precursors. The quantity to compare is not the
 * window width but points per peak: the peak width divided by the median gap between consecutive visits.
 * <pre>
 * SIRIUS_DIA_TEST_FILE=/path/a.mzML,/path/b.mzML \
 *   ./gradlew :lcms2:cleanTest :lcms2:test --tests '*Ms2WindowRecurrenceFileTest*' -q -i
 * </pre>
 */
public class Ms2WindowRecurrenceFileTest {

    /** what a peak is assumed to span, in seconds, where the sample does not say */
    private static final double ASSUMED_PEAK_WIDTH = 15d;

    @Test
    public void windowRecurrenceAndDensity() throws IOException {
        final List<Path> files = new ArrayList<>();
        for (String part : System.getenv().getOrDefault("SIRIUS_DIA_TEST_FILE", "").split(",")) {
            if (!part.isBlank() && Files.exists(Path.of(part.trim()))) files.add(Path.of(part.trim()));
        }
        assumeTrue(!files.isEmpty(), "no file given, set SIRIUS_DIA_TEST_FILE");

        for (Path file : files) {
            final LCMSProcessing processing = new LCMSProcessing(new de.unijena.bioinf.lcms.projectspace.CollectingDatabaseAdapter(), false);
            final ProcessedSample sample = processing.processSample(file);
            // the measured per sample peak width, which TraceStats derives from the MS1 trace segments and
            // makeMergeStatistics later robust-averages into the merged expectedPeakWidth. The merged one is not
            // set until after merging, so it is empty here and would silently become the assumed constant.
            final double measured = sample.getTraceStats() == null ? 0d
                    : sample.getTraceStats().getAveragePeakWidth();
            final double peakWidth = measured > 0 ? measured : ASSUMED_PEAK_WIDTH;

            // retention times per quantized absolute window, exactly as DiaChannelKey groups them
            final Long2ObjectOpenHashMap<DoubleArrayList> byWindow = new Long2ObjectOpenHashMap<>();
            int ms2 = 0;
            double firstRt = Double.POSITIVE_INFINITY, lastRt = Double.NEGATIVE_INFINITY;
            for (Ms2SpectrumHeader header : sample.getStorage().getSpectrumStorage().ms2SpectraHeader()) {
                final IsolationWindow window = header.getIsolationWindow().orElse(null);
                if (window == null || window.isUndefined()) continue;
                ++ms2;
                double target = header.getTargetedMz();
                if (!Double.isFinite(target) || target <= 0) target = header.getPrecursorMz();
                final long lo = Math.round((float) (target + window.getLeftOffset()) * 10d);
                final long hi = Math.round((float) (target + window.getRightOffset()) * 10d);
                byWindow.computeIfAbsent((lo << 24) ^ hi, k -> new DoubleArrayList()).add(header.getRetentionTime());
                firstRt = Math.min(firstRt, header.getRetentionTime());
                lastRt = Math.max(lastRt, header.getRetentionTime());
            }

            System.out.printf(Locale.US, "%n=== %s ===%n", file.getFileName());
            System.out.printf(Locale.US, "%d MS2 with a window, %d distinct windows, run %.0f s, peak width %.1f s (%s)%n",
                    ms2, byWindow.size(), lastRt - firstRt, peakWidth, measured > 0 ? "measured" : "ASSUMED");
            System.out.printf(Locale.US, "%n  %8s %8s %10s %10s %9s%n",
                    "windows", "scans", "median gap", "pts/peak", "span %");
            // bucketed by how many points per peak the window manages, which is the quantity a rule would use
            final int[] windowsPerBand = new int[6];
            final int[] scansPerBand = new int[6];
            for (DoubleArrayList rts : byWindow.values()) {
                final double[] sorted = rts.toDoubleArray();
                Arrays.sort(sorted);
                final int band = bandOf(sorted.length < 2 ? 0d : peakWidth / medianGap(sorted));
                ++windowsPerBand[band];
                scansPerBand[band] += sorted.length;
            }
            for (int band = 0; band < windowsPerBand.length; ++band) {
                if (windowsPerBand[band] == 0) continue;
                System.out.printf(Locale.US, "  %-22s %6d windows %8d scans (%4.1f%% of MS2)%n",
                        BAND_NAMES[band], windowsPerBand[band], scansPerBand[band],
                        100d * scansPerBand[band] / Math.max(1, ms2));
            }
            processing.closeStorages();
        }
    }

    private static final String[] BAND_NAMES = {"<1 pt/peak", "1-2 pts/peak", "2-3 pts/peak", "3-5 pts/peak",
            "5-10 pts/peak", ">=10 pts/peak"};

    private static int bandOf(double pointsPerPeak) {
        if (pointsPerPeak < 1) return 0;
        if (pointsPerPeak < 2) return 1;
        if (pointsPerPeak < 3) return 2;
        if (pointsPerPeak < 5) return 3;
        if (pointsPerPeak < 10) return 4;
        return 5;
    }

    private static double medianGap(double[] sortedRts) {
        final double[] gaps = new double[sortedRts.length - 1];
        for (int k = 1; k < sortedRts.length; ++k) gaps[k - 1] = sortedRts[k] - sortedRts[k - 1];
        Arrays.sort(gaps);
        return Math.max(1e-9, gaps[gaps.length / 2]);
    }
}
