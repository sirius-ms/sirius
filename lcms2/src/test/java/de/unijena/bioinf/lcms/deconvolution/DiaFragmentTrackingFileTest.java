package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.utils.TrackDiaFragmentsToLog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static de.unijena.bioinf.lcms.deconvolution.DiaPipelineFileTest.files;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Follows a few fragment masses of one compound through the DIA assignment and prints where each of them was lost.
 * <p>
 * Aggregate agreement with a reference measurement says how much is lost and at best narrows down what kind of peak
 * it happens to. It cannot say at which step, and the steps are many: a fragment has to be above the channel's noise
 * level in a scan, be followable across enough scan points to become a chromatogram, overlap the precursor feature
 * and peak near it to become a candidate, clear a significance floor measured on decoys, win enough probability
 * against the other precursors of the window, and finally be given a share of its own intensity. Every one of those
 * can drop it, and the fix is different for each.
 * <p>
 * The masses worth tracking are the ones another tool found in the same file and a DDA measurement of the same
 * compound confirms, since those are known to be in the raw data. {@code MzmineVersusDdaFileTest} prints them with
 * {@code SIRIUS_DIA_DUMP_DIVERGENT=true}, and the defaults here are one such compound.
 * <pre>
 * SIRIUS_DIA_TEST_FILE=/path/fixed/SWATH_1.mzML,... \
 *   [SIRIUS_TRACK_PRECURSOR=237.1486 SIRIUS_TRACK_RT=552.5 SIRIUS_TRACK_FRAGMENTS=133.1013,201.1275] \
 *   ./gradlew :lcms2:cleanTest :lcms2:test --tests '*DiaFragmentTrackingFileTest*'
 * </pre>
 */
public class DiaFragmentTrackingFileTest {

    /** how far from the given retention time a scan or a feature still belongs to the compound, in seconds */
    private static final double DEFAULT_RT_TOLERANCE = 20d;

    /**
     * m/z 237.1486 at 552.5 s in the SWATH files of MSV000088719: the DDA measurement of that compound has 99
     * fragment peaks, MZmine finds 96 of which 73 are confirmed by it, and we report three of which none is.
     */
    private static final double DEFAULT_PRECURSOR = 237.1486;
    private static final double DEFAULT_RETENTION_TIME = 552.5;
    /** the four strongest masses MZmine and the DDA reference agree on and we do not have */
    private static final String DEFAULT_FRAGMENTS = "133.1013,201.1275,191.1431,119.0858";

    @Test
    public void trackFragmentsOfOneCompound() throws IOException {
        final List<Path> diaFiles = files("SIRIUS_DIA_TEST_FILE");
        assumeTrue(!diaFiles.isEmpty(), "no DIA file given, set SIRIUS_DIA_TEST_FILE");

        final double precursorMz = Double.parseDouble(
                System.getenv().getOrDefault("SIRIUS_TRACK_PRECURSOR", String.valueOf(DEFAULT_PRECURSOR)));
        final double retentionTime = Double.parseDouble(
                System.getenv().getOrDefault("SIRIUS_TRACK_RT", String.valueOf(DEFAULT_RETENTION_TIME)));
        final String[] parts = System.getenv().getOrDefault("SIRIUS_TRACK_FRAGMENTS", DEFAULT_FRAGMENTS).split(",");
        final double[] fragments = new double[parts.length];
        for (int k = 0; k < parts.length; ++k) fragments[k] = Double.parseDouble(parts[k].trim());

        final TrackDiaFragmentsToLog tracker = new TrackDiaFragmentsToLog(precursorMz, retentionTime,
                DEFAULT_RT_TOLERANCE, fragments);
        PipelineRun.run(diaFiles, "tracked", processing -> processing.setTracker(tracker), true);
        final String report = tracker.describe();
        System.out.println(report);
        assertTrue(report.contains("fragment"), "the tracker has to report a table even when it saw nothing");
    }
}
