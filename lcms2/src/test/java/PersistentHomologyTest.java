import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.lcms.trace.segmentation.PersistentHomology;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

public class PersistentHomologyTest {

    private PersistentHomology persistentHomology;
    private Trace testTrace;
    private SampleStats sampleStats;

    @BeforeEach
    public void setUp() {
        // Initialize the PersistentHomology object
        persistentHomology = new PersistentHomology();
        final double[] retentionTimes = new double[50];
        final int[] scanIds = new int[50];
        final double stepWidth = 1.3, offset=6.3;
        for (int i=0; i  < retentionTimes.length; ++i) {
            retentionTimes[i] = offset + stepWidth*i;
            scanIds[i] = i;
        }
        ScanPointMapping mockMapping = new ScanPointMapping(retentionTimes, scanIds, null);

        {
            final double pmz = 167.012;
            final double[] mzarray = new double[10];
            Arrays.fill(mzarray, pmz);
            final float[] intensities = new float[]{
                40, 10, 60, 720, 1520, 690, 310, 20, 10, 30
            };

            // Initialize a sample Trace object (example trace data)
            testTrace = new ContiguousTrace(mockMapping, 30, 30+mzarray.length-1, mzarray, intensities);
        }

        {
            sampleStats = SampleStats.builder().expectedPeakWidth(0).noiseLevelPerScan(new float[testTrace.apex() + 1]).build();
            sampleStats.getNoiseLevelPerScan()[testTrace.apex()] = 20;
        }
    }

    @Test
    public void testDetectSegmentsWithNoiseLevel() {
        // Call the method to test
        List<TraceSegment> segments = persistentHomology.detectSegments(sampleStats, testTrace, new int[0]);

        // Assert the segments detected
        assertNotNull(segments);
        assertEquals(1, segments.size(), "Expected one segment to be detected");

        TraceSegment segment = segments.getFirst();
        assertTrue(segment.leftEdge <= 31);
        assertEquals(34, segment.apex);
        assertTrue(segment.rightEdge >= 37);
    }


//    @Test
    public void testDetectMaxima() {
        // Call the method to detect maxima
        int[] maxima = persistentHomology.detectMaxima(sampleStats, testTrace, new int[0]);

        // Assert the maxima detected
        assertNotNull(maxima);
        assertEquals(1, maxima.length, "Expected one maximum (apex) to be detected");
        assertEquals(3, maxima[0], "Expected apex index to be 3");
    }


}

