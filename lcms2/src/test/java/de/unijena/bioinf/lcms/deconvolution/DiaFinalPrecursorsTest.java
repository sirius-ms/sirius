package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.align.RecalibrationFunction;
import de.unijena.bioinf.lcms.merge.ScanPointInterpolator;
import de.unijena.bioinf.lcms.msms.MsMsTraceReference;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.ProjectedTrace;
import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The two rules stage two has to reproduce exactly: how a feature of the merged trace becomes a window in one
 * sample's own scan indices, and which pseudo spectrum will end up in which feature.
 * <p>
 * Both are transcriptions of what the importer and the MS2 merge strategy do afterwards. If they drift apart, stage
 * two measures over one window and the result is attached to another, which is not a failure that shows up as a
 * missing spectrum - it shows up as a plausible spectrum on the wrong compound.
 */
public class DiaFinalPrecursorsTest {

    /** the merged axis: twice as many scan points as the sample, half a second apart */
    private static final int MERGED_POINTS = 100;
    /** the sample's own axis: one second apart, so merged index 2k is sample index k */
    private static final int SAMPLE_POINTS = 50;

    private LCMSStorageFactory factory;

    @AfterEach
    public void tearDown() {
        if (factory != null) factory.close();
    }

    private static ScanPointMapping axis(int points, double step) {
        final double[] retentionTimes = new double[points];
        final int[] scanIds = new int[points];
        for (int k = 0; k < points; ++k) {
            retentionTimes[k] = k * step;
            scanIds[k] = k;
        }
        return new ScanPointMapping(retentionTimes, scanIds, null);
    }

    private ProcessedSample sample() throws IOException {
        factory = LCMSStorage.temporaryStorage(null, false);
        final ScanPointMapping merged = axis(MERGED_POINTS, 0.5);
        final ProcessedSample sample = new ProcessedSample(axis(SAMPLE_POINTS, 1.0), factory.createNewStorage(), 1, 0);
        sample.setScanPointInterpolator(new ScanPointInterpolator(merged, sample.getMapping(),
                RecalibrationFunction.identity()));
        return sample;
    }

    private static ProcessedSample mergedSample() {
        return new ProcessedSample(axis(MERGED_POINTS, 0.5), null, 1, 1);
    }

    /**
     * A trace measured over raw scans 2 to 20, peaking at raw scan 9, plus a second, lower peak at raw 17. The
     * second one is what makes a wrong apex visible: it is the largest point of a window that excludes scan 9.
     */
    private static ProjectedTrace projectedTrace() {
        final int rawFrom = 2, rawTo = 20, projectedFrom = 4, projectedTo = 40;
        final float[] rawIntensities = new float[rawTo - rawFrom + 1];
        for (int k = rawFrom; k <= rawTo; ++k) {
            rawIntensities[k - rawFrom] = k == 17 ? 800f : (float) (1000d - 100d * Math.abs(k - 9));
        }
        final float[] projectedIntensities = new float[projectedTo - projectedFrom + 1];
        for (int k = projectedFrom; k <= projectedTo; ++k) {
            projectedIntensities[k - projectedFrom] = (float) (1000d - 50d * Math.abs(k - 18));
        }
        return new ProjectedTrace(0, rawFrom, rawTo, 9, projectedFrom, projectedTo, 18, 300.1234,
                new float[rawIntensities.length], new float[projectedIntensities.length],
                rawIntensities, projectedIntensities);
    }

    @Test
    public void windowIsTranslatedIntoTheSamplesOwnScanIndices() throws IOException {
        final ProcessedSample sample = sample();
        final ProjectedTrace projected = projectedTrace();
        final Trace raw = projected.raw(sample.getMapping());

        // merged scans 12 to 30 are the sample's scans 6 to 15, at half the resolution
        final TraceSegment window = DiaFinalPrecursors.rawWindowOf(projected, sample,
                new TraceSegment(20, 12, 30), raw);
        assertEquals(6, window.leftEdge, "the left edge maps to the sample's own axis");
        assertEquals(15, window.rightEdge, "and so does the right edge");
    }

    @Test
    public void windowIsClampedToWhatTheSampleMeasured() throws IOException {
        final ProcessedSample sample = sample();
        final ProjectedTrace projected = projectedTrace();
        final Trace raw = projected.raw(sample.getMapping());

        // a feature spanning the whole merged trace cannot reach beyond the scans this sample contributed
        final TraceSegment window = DiaFinalPrecursors.rawWindowOf(projected, sample,
                new TraceSegment(50, 0, MERGED_POINTS - 1), raw);
        assertEquals(projected.getRawStartId(), window.leftEdge);
        assertEquals(projected.getRawEndId(), window.rightEdge);
    }

    @Test
    public void apexComesFromTheSampleNotFromTheMergedTrace() throws IOException {
        final ProcessedSample sample = sample();
        final ProjectedTrace projected = projectedTrace();
        final Trace raw = projected.raw(sample.getMapping());

        // the merged apex is at 20, which is the sample's scan 10, but this sample peaks at 9
        final TraceSegment window = DiaFinalPrecursors.rawWindowOf(projected, sample,
                new TraceSegment(20, 12, 30), raw);
        assertEquals(9, window.apex, "the apex is where this sample peaks");

        // and within a window that excludes the real apex it is the largest point that is left
        final TraceSegment later = DiaFinalPrecursors.rawWindowOf(projected, sample,
                new TraceSegment(28, 24, 34), raw);
        assertEquals(12, later.leftEdge);
        assertEquals(17, later.apex, "not the global apex, which is outside this window");
    }

    /** registers a pseudo spectrum and returns a reference to it, as the deconvolution would have written it */
    private static MsMsTraceReference pseudoSpectrum(ProcessedSample sample, double retentionTime, int parentId) {
        final SimpleMutableSpectrum buffer = new SimpleMutableSpectrum();
        buffer.addPeak(100.1, 1000);
        final Ms2SpectrumHeader header = new Ms2SpectrumHeader(
                DiaPseudoSpectra.SOURCE_ID_PREFIX + parentId, parentId, 1, 2, true,
                new CollisionEnergy(35), new IsolationWindow(0, 100), parentId, 300.1234, 300.1234, retentionTime);
        final Ms2SpectrumHeader stored = sample.getStorage().getSpectrumStorage()
                .addMs2Spectrum(header, new SimpleSpectrum(buffer));
        return new MsMsTraceReference(stored.getUid(), 0, parentId, parentId);
    }

    /** the same, but as the instrument would have written it: an acquired scan, not a computed spectrum */
    private static MsMsTraceReference acquiredSpectrum(ProcessedSample sample, double retentionTime, int parentId) {
        final SimpleMutableSpectrum buffer = new SimpleMutableSpectrum();
        buffer.addPeak(100.1, 1000);
        final Ms2SpectrumHeader header = new Ms2SpectrumHeader("scan=" + parentId, parentId, 1, 2, true,
                new CollisionEnergy(35), new IsolationWindow(0, 100), parentId, 300.1234, 300.1234, retentionTime);
        final Ms2SpectrumHeader stored = sample.getStorage().getSpectrumStorage()
                .addMs2Spectrum(header, new SimpleSpectrum(buffer));
        return new MsMsTraceReference(stored.getUid(), 0, parentId, parentId);
    }

    @Test
    public void spectraGoToTheSegmentClosestInRetentionTime() throws IOException {
        final ProcessedSample sample = sample();
        final ProcessedSample merged = mergedSample();
        final ProjectedTrace projected = projectedTrace();

        // merged scans 8-12 are 4 to 6 seconds, merged scans 28-32 are 14 to 16 seconds
        final TraceSegment[] segments = {new TraceSegment(10, 8, 12), new TraceSegment(30, 28, 32)};
        final MsMsTraceReference inside = pseudoSpectrum(sample, 5.0, 5);
        final MsMsTraceReference second = pseudoSpectrum(sample, 15.0, 15);
        final MsMsTraceReference beyond = pseudoSpectrum(sample, 30.0, 25);
        projected.setMs2Refs(new MsMsTraceReference[]{inside, second, beyond});

        final int[][] perSegment = DiaFinalPrecursors.assignSpectraToSegments(merged, sample, projected, segments,
                segments);
        assertArrayEquals(new int[]{inside.ms2Uid}, perSegment[0]);
        // the one beyond every segment still lands somewhere, at the closest one, exactly as the merge does
        assertArrayEquals(new int[]{second.ms2Uid, beyond.ms2Uid}, perSegment[1]);
    }

    @Test
    public void segmentsOfTheMergedTraceAreTheFallback() throws IOException {
        final ProcessedSample sample = sample();
        final ProcessedSample merged = mergedSample();
        final ProjectedTrace projected = projectedTrace();

        final TraceSegment[] mergedSegments = {new TraceSegment(10, 8, 12), new TraceSegment(30, 28, 32)};
        final MsMsTraceReference reference = pseudoSpectrum(sample, 15.0, 15);
        projected.setMs2Refs(new MsMsTraceReference[]{reference});

        // this sample has no segment of its own for either feature
        final int[][] perSegment = DiaFinalPrecursors.assignSpectraToSegments(merged, sample, projected,
                new TraceSegment[]{null, null}, mergedSegments);
        assertEquals(0, perSegment[0].length);
        assertArrayEquals(new int[]{reference.ms2Uid}, perSegment[1]);
    }

    @Test
    public void acquiredSpectraAreNotTouched() throws IOException {
        final ProcessedSample sample = sample();
        final ProcessedSample merged = mergedSample();
        final ProjectedTrace projected = projectedTrace();

        final TraceSegment[] segments = {new TraceSegment(10, 8, 12)};
        // a real MS2 scan of a DDA sample must not be mistaken for something we may overwrite
        projected.setMs2Refs(new MsMsTraceReference[]{acquiredSpectrum(sample, 5.0, 5)});

        final int[][] perSegment = DiaFinalPrecursors.assignSpectraToSegments(merged, sample, projected, segments,
                segments);
        assertEquals(0, perSegment[0].length);
    }

    @Test
    public void aTraceWithoutSpectraIsNoProblem() throws IOException {
        final ProcessedSample sample = sample();
        final ProcessedSample merged = mergedSample();
        final ProjectedTrace projected = projectedTrace();
        // merging leaves the references null for a trace that never had any
        final int[][] perSegment = DiaFinalPrecursors.assignSpectraToSegments(merged, sample, projected,
                new TraceSegment[]{new TraceSegment(10, 8, 12)}, new TraceSegment[]{new TraceSegment(10, 8, 12)});
        assertEquals(0, perSegment[0].length);
    }
}
