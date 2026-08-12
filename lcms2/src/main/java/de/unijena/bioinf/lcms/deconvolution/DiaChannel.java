package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.lcms.ScanPointMapping;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * One DIA acquisition channel: all MS2 scans of a run that isolate the same set of m/z ranges at the same
 * collision energy.
 * <p>
 * The point of a channel is that its scans form a regular time series of their own. A fragment chromatogram
 * can therefore be picked within a channel exactly the way an MS1 trace is picked within a sample - along
 * consecutive indices of {@link #getAxis()} - without ever having to deal with the scans of the other windows.
 * Note that a channel usually fires much less often than the MS1 scans (once per cycle of all windows), so its
 * axis is considerably coarser than the sample's MS1 axis. Everything that reasons about peak width therefore
 * has to go through the retention times of the axis and must not count scan points.
 */
public class DiaChannel {

    @Getter
    private final int channelId;

    /**
     * absolute m/z bounds of the co-isolated windows, ascending in {@code windowLo}. Float is plenty here: an
     * isolation window is wide and the instrument does not realize its bounds precisely.
     */
    private final float[] windowLo, windowHi;

    @Getter
    @Nullable
    private final CollisionEnergy collisionEnergy;

    /**
     * retention times of this channel's MS2 scans (ascending) mapped to the uid of the corresponding
     * {@link de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader}
     */
    @Getter
    private final ScanPointMapping axis;

    /** for every index of {@link #getAxis()} the index of the MS1 scan the fragments belong to */
    @Getter
    private final int[] toMs1Index;

    public DiaChannel(int channelId, float[] windowLo, float[] windowHi, @Nullable CollisionEnergy collisionEnergy,
                      ScanPointMapping axis, int[] toMs1Index) {
        if (windowLo.length != windowHi.length)
            throw new IllegalArgumentException("window bounds must be of equal length");
        if (axis.length() != toMs1Index.length)
            throw new IllegalArgumentException("there must be exactly one MS1 index per scan point of the channel");
        this.channelId = channelId;
        this.windowLo = windowLo;
        this.windowHi = windowHi;
        this.collisionEnergy = collisionEnergy;
        this.axis = axis;
        this.toMs1Index = toMs1Index;
    }

    /**
     * @return number of MS2 scans in this channel, i.e. the length of its retention time axis
     */
    /**
     * The identity of this channel, independent of the sample it was reconstructed in. Channel ids are handed out
     * per sample and say nothing across samples; this is what says that two samples measured the same thing.
     */
    DiaChannelKey key() {
        final float[] bounds = new float[2 * numberOfWindows()];
        for (int k = 0; k < numberOfWindows(); ++k) {
            bounds[2 * k] = windowLowerBound(k);
            bounds[2 * k + 1] = windowUpperBound(k);
        }
        return DiaChannelKey.of(bounds, getCollisionEnergy());
    }

    public int numberOfScans() {
        return axis.length();
    }

    /**
     * @return number of co-isolated m/z ranges. Larger than one for multiplexed (MSX) acquisitions.
     */
    public int numberOfWindows() {
        return windowLo.length;
    }

    public float windowLowerBound(int windowIndex) {
        return windowLo[windowIndex];
    }

    public float windowUpperBound(int windowIndex) {
        return windowHi[windowIndex];
    }

    /**
     * @return true if the given precursor m/z falls into any of the co-isolated windows
     */
    public boolean covers(double precursorMz) {
        return windowIndexFor(precursorMz) >= 0;
    }

    /**
     * @return index of the co-isolated window containing the given precursor m/z, or -1. For a precursor that
     * falls into more than one window (which cannot happen within a sane acquisition scheme) the first is returned.
     */
    public int windowIndexFor(double precursorMz) {
        for (int k = 0; k < windowLo.length; ++k) {
            if (precursorMz >= windowLo[k] && precursorMz <= windowHi[k]) return k;
        }
        return -1;
    }

    /**
     * The isolation window as it should be reported for a concrete precursor, i.e. relative to that precursor
     * rather than to the centre of the acquisition window.
     *
     * @return the window containing the precursor, or null if no window does
     */
    @Nullable
    public IsolationWindow isolationWindowFor(double precursorMz) {
        final int k = windowIndexFor(precursorMz);
        if (k < 0) return null;
        return IsolationWindow.fromOffsets(precursorMz - windowLo[k], windowHi[k] - precursorMz);
    }

    /**
     * @return index of this channel's scan point closest in retention time to the given MS1 scan index, based on
     * the retention time of that MS1 scan
     */
    public int channelIndexForRetentionTime(double retentionTime) {
        return axis.idForRetentionTime(retentionTime);
    }

    /**
     * @return uid of the {@link de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader} at the given index of the axis
     */
    public int ms2UidAt(int channelIndex) {
        return axis.getScanIdAt(channelIndex);
    }

    /**
     * @return median retention time distance between two consecutive scans of this channel, in seconds. This is
     * the sampling rate available for fragment chromatograms and is typically the cycle time of the acquisition.
     */
    public double medianScanInterval() {
        final int n = numberOfScans();
        if (n < 2) return 0d;
        final double[] deltas = new double[n - 1];
        for (int k = 1; k < n; ++k) deltas[k - 1] = axis.getRetentionTimeAt(k) - axis.getRetentionTimeAt(k - 1);
        java.util.Arrays.sort(deltas);
        return deltas[deltas.length / 2];
    }

    public float lowestMz() {
        return windowLo[0];
    }

    public float highestMz() {
        float max = windowHi[0];
        for (float hi : windowHi) max = Math.max(max, hi);
        return max;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("DiaChannel ").append(channelId).append(" [");
        for (int k = 0; k < windowLo.length; ++k) {
            if (k > 0) buf.append(", ");
            buf.append(String.format(java.util.Locale.US, "%.1f-%.1f", windowLo[k], windowHi[k]));
        }
        buf.append("]");
        if (collisionEnergy != null) buf.append(" @").append(collisionEnergy);
        buf.append(", ").append(numberOfScans()).append(" scans");
        return buf.toString();
    }
}
