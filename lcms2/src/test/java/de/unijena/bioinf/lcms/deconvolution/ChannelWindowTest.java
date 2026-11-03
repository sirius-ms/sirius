package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Translating a preliminary MS1 feature onto the scan points of a channel. The channel ticks about ten times
 * slower than the MS1 scans, so a feature that is comfortably sampled in MS1 can be too narrow here.
 */
public class ChannelWindowTest {

    private static final double MS1_STEP = 0.2;
    private static final double CHANNEL_STEP = 2.24;

    private static ScanPointMapping axis(double startRt, double step, int length) {
        final double[] rts = new double[length];
        final int[] ids = new int[length];
        for (int k = 0; k < length; ++k) {
            rts[k] = startRt + k * step;
            ids[k] = k;
        }
        return new ScanPointMapping(rts, ids, null);
    }

    private static final ScanPointMapping MS1 = axis(0d, MS1_STEP, 1000);

    private static DiaChannel channel(double startRt, int length) {
        ScanPointMapping ax = axis(startRt, CHANNEL_STEP, length);
        final int[] toMs1 = new int[length];
        for (int k = 0; k < length; ++k) toMs1[k] = Math.min(MS1.length() - 1, (int) Math.round(ax.getRetentionTimeAt(k) / MS1_STEP));
        return new DiaChannel(0, new float[]{350f}, new float[]{450f}, new CollisionEnergy(35), ax, toMs1);
    }

    private static ContiguousTrace flatTrace() {
        final double[] mz = new double[MS1.length()];
        final float[] intensity = new float[MS1.length()];
        for (int k = 0; k < mz.length; ++k) {
            mz[k] = 400.2;
            intensity[k] = 100f;
        }
        return new ContiguousTrace(MS1, 0, MS1.length() - 1, mz, intensity);
    }

    /** a segment given in seconds, converted to MS1 indices */
    private static TraceSegment segment(double startRt, double apexRt, double endRt) {
        return new TraceSegment((int) Math.round(apexRt / MS1_STEP), (int) Math.round(startRt / MS1_STEP),
                (int) Math.round(endRt / MS1_STEP));
    }

    @Test
    public void aBroadFeatureCoversSeveralScanPointsOfTheChannel() {
        // 20 s wide, so roughly nine channel scans
        Optional<ChannelWindow> window = ChannelWindow.of(channel(0d, 90), flatTrace(), segment(50d, 60d, 70d));

        assertTrue(window.isPresent());
        assertTrue(window.get().length() >= 8, "expected about nine scan points, got " + window.get().length());
        // and it has to sit where the feature is
        ScanPointMapping ax = channel(0d, 90).getAxis();
        assertEquals(50d, ax.getRetentionTimeAt(window.get().from()), CHANNEL_STEP);
        assertEquals(70d, ax.getRetentionTimeAt(window.get().to()), CHANNEL_STEP);
    }

    /**
     * A feature of 2 s is 10 MS1 scan points, a perfectly respectable peak, and still too narrow for this
     * channel to describe a shape.
     */
    @Test
    public void aFeatureTooNarrowForTheChannelYieldsNoWindow() {
        assertTrue(ChannelWindow.of(channel(0d, 90), flatTrace(), segment(59d, 60d, 61d)).isEmpty());
    }

    @Test
    public void aFeatureOutsideWhatTheChannelMeasuredYieldsNoWindow() {
        // the channel only runs from 100 s onwards, the feature elutes at 60 s
        assertTrue(ChannelWindow.of(channel(100d, 50), flatTrace(), segment(50d, 60d, 70d)).isEmpty());
        // and the other way round: the channel stops long before the feature
        assertTrue(ChannelWindow.of(channel(0d, 10), flatTrace(), segment(150d, 160d, 170d)).isEmpty());
    }

    @Test
    public void windowsKnowWhetherTheyOverlapAndCanBeUnited() {
        ChannelWindow a = new ChannelWindow(10, 20);
        ChannelWindow b = new ChannelWindow(18, 30);
        ChannelWindow c = new ChannelWindow(40, 50);

        assertTrue(a.overlaps(b));
        assertTrue(b.overlaps(a));
        assertFalse(a.overlaps(c));

        assertEquals(new ChannelWindow(10, 30), a.union(b));
        assertEquals(new ChannelWindow(10, 50), a.union(c), "a union does not require an overlap");
        assertEquals(11, a.length());
        assertTrue(a.contains(10) && a.contains(20) && !a.contains(21));
    }

    @Test
    public void anEmptyWindowIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ChannelWindow(20, 19));
    }
}
