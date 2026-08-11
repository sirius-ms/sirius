package de.unijena.bioinf.lcms.trace.segmentation;

import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NoSegmentationTest {

    private static ContiguousTrace twoPeaks() {
        final int n = 40;
        final double[] rts = new double[n];
        final int[] ids = new int[n];
        for (int k = 0; k < n; ++k) {
            rts[k] = k * 2.24;
            ids[k] = k;
        }
        final ScanPointMapping axis = new ScanPointMapping(rts, ids, null);
        final double[] mz = new double[n];
        final float[] intensity = new float[n];
        for (int k = 0; k < n; ++k) {
            mz[k] = 150.1;
            intensity[k] = (float) (1000 * Math.exp(-0.5 * Math.pow((k - 10) / 2d, 2))
                    + 800 * Math.exp(-0.5 * Math.pow((k - 30) / 2d, 2)) + 5);
        }
        return new ContiguousTrace(axis, 0, n - 1, mz, intensity);
    }

    /** Two clearly separated peaks, and still exactly one segment: that is the point of this strategy. */
    @Test
    public void theWholeTraceBecomesOneSegment() {
        ContiguousTrace trace = twoPeaks();
        List<TraceSegment> segments = new NoSegmentation().detectSegments(trace, 10d, 5d, new int[0], new int[0]);

        assertEquals(1, segments.size());
        assertEquals(trace.startId(), segments.getFirst().leftEdge);
        assertEquals(trace.endId(), segments.getFirst().rightEdge);
        assertEquals(trace.apex(), segments.getFirst().apex);
    }

    /** For comparison: a real segmenter finds both peaks in the same trace. */
    @Test
    public void aRealSegmenterWouldSplitTheSameTrace() {
        ContiguousTrace trace = twoPeaks();
        assertTrue(new PersistentHomology().detectSegments(trace, 10d, 5d, new int[0], new int[0]).size() > 1,
                "precondition: this trace really does hold more than one feature");
    }

    @Test
    public void noiseLevelAndPeakWidthAreIrrelevant() {
        ContiguousTrace trace = twoPeaks();
        NoSegmentation strategy = new NoSegmentation();
        assertEquals(1, strategy.detectSegments(trace, 0d, 0d, new int[0], new int[0]).size());
        assertEquals(1, strategy.detectSegments(trace, 1e9, 1e9, new int[0], new int[0]).size(),
                "not even a noise level above every peak removes the segment");
    }
}
