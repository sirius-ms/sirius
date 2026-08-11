package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;

import java.util.Arrays;
import java.util.Locale;

/**
 * Identity of a DIA acquisition channel: the set of m/z ranges that are co-isolated in one MS2 scan, together
 * with the collision energy they were fragmented with.
 * <p>
 * Two details matter here and both are easy to get wrong:
 * <ul>
 *     <li>{@link de.unijena.bioinf.ChemistryBase.ms.IsolationWindow} stores its bounds <b>relative</b> to the
 *     precursor m/z, so all windows of a SWATH scheme with equal width look identical. The channel identity
 *     therefore has to be built from <b>absolute</b> m/z bounds.</li>
 *     <li>Co-isolated precursors of a multiplexed (MSX) scan are not listed in any reliable order, so the
 *     bounds are sorted into a canonical order before they become part of the identity.</li>
 * </ul>
 * Bounds are quantized before comparison because vendors do not write bit-identical values for the same nominal
 * window across scans. Isolation windows are wide and the instrument does not realize them precisely, so a tenth
 * of a Dalton is more resolution than the numbers actually carry - and far more than we need to tell two windows
 * of an acquisition scheme apart.
 */
final class DiaChannelKey implements Comparable<DiaChannelKey> {

    /**
     * A tenth of a Dalton. Note that this is plain rounding, not clustering, so two values could in principle
     * still end up in different buckets if they straddle a bucket boundary. Real window bounds sit nowhere near
     * such a boundary; they differ only by float noise, if at all.
     */
    private static final double QUANTIZATION = 10d;

    private static final int UNKNOWN_ENERGY = Integer.MIN_VALUE;

    /** quantized m/z bounds, interleaved as [lo0,hi0,lo1,hi1,...] and ascending in lo */
    private final int[] bounds;

    private final int minEnergy, maxEnergy;

    private final int hash;

    private DiaChannelKey(int[] bounds, int minEnergy, int maxEnergy) {
        this.bounds = bounds;
        this.minEnergy = minEnergy;
        this.maxEnergy = maxEnergy;
        this.hash = 31 * (31 * Arrays.hashCode(bounds) + minEnergy) + maxEnergy;
    }

    /**
     * @param absoluteBounds absolute m/z bounds of all co-isolated windows, interleaved as [lo,hi,lo,hi,...]
     *                       and in arbitrary order
     * @param collisionEnergy may be null or undefined
     */
    static DiaChannelKey of(float[] absoluteBounds, CollisionEnergy collisionEnergy) {
        if (absoluteBounds.length == 0 || (absoluteBounds.length % 2) != 0)
            throw new IllegalArgumentException("expected a non-empty interleaved [lo,hi] array, got length " + absoluteBounds.length);
        final int n = absoluteBounds.length / 2;
        final int[] quantized = new int[absoluteBounds.length];
        for (int k = 0; k < n; ++k) {
            int lo = quantize(absoluteBounds[2 * k]);
            int hi = quantize(absoluteBounds[2 * k + 1]);
            // tolerate a window that is handed to us the wrong way round
            quantized[2 * k] = Math.min(lo, hi);
            quantized[2 * k + 1] = Math.max(lo, hi);
        }
        sortPairsByLowerBound(quantized);
        return new DiaChannelKey(quantized, energyOf(collisionEnergy, true), energyOf(collisionEnergy, false));
    }

    private static int quantize(double value) {
        return (int) Math.round(value * QUANTIZATION);
    }

    private static int energyOf(CollisionEnergy energy, boolean min) {
        if (energy == null) return UNKNOWN_ENERGY;
        final double value = min ? energy.minEnergySource() : energy.maxEnergySource();
        // an unknown collision energy is written as NaN or as a negative value, depending on where it came from
        if (!Double.isFinite(value) || value < 0) return UNKNOWN_ENERGY;
        return quantize(value);
    }

    /** insertion sort over (lo,hi) pairs; there are only a handful of windows per scan */
    private static void sortPairsByLowerBound(int[] interleaved) {
        for (int i = 2; i < interleaved.length; i += 2) {
            final int lo = interleaved[i], hi = interleaved[i + 1];
            int j = i - 2;
            while (j >= 0 && interleaved[j] > lo) {
                interleaved[j + 2] = interleaved[j];
                interleaved[j + 3] = interleaved[j + 1];
                j -= 2;
            }
            interleaved[j + 2] = lo;
            interleaved[j + 3] = hi;
        }
    }

    /**
     * @return the m/z bounds in canonical order, interleaved as [lo0,hi0,lo1,hi1,...]. These are the quantized
     * values, so they may differ from the ones in the file by up to 0.05 m/z. Using them rather than the raw
     * bounds of an arbitrarily chosen scan keeps a channel's bounds consistent with its identity.
     */
    float[] canonicalBounds() {
        final float[] result = new float[bounds.length];
        for (int k = 0; k < bounds.length; ++k) result[k] = (float) (bounds[k] / QUANTIZATION);
        return result;
    }

    /**
     * Total order over channels so that the channel list of a sample does not depend on the order in which the
     * scans happened to be read.
     */
    @Override
    public int compareTo(DiaChannelKey o) {
        for (int k = 0; k < Math.min(bounds.length, o.bounds.length); ++k) {
            int c = Integer.compare(bounds[k], o.bounds[k]);
            if (c != 0) return c;
        }
        int c = Integer.compare(bounds.length, o.bounds.length);
        if (c != 0) return c;
        c = Integer.compare(minEnergy, o.minEnergy);
        if (c != 0) return c;
        return Integer.compare(maxEnergy, o.maxEnergy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiaChannelKey that)) return false;
        return minEnergy == that.minEnergy && maxEnergy == that.maxEnergy && Arrays.equals(bounds, that.bounds);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int k = 0; k < bounds.length; k += 2) {
            if (k > 0) buf.append('+');
            buf.append(String.format(Locale.US, "%.1f-%.1f", bounds[k] / QUANTIZATION, bounds[k + 1] / QUANTIZATION));
        }
        if (minEnergy != UNKNOWN_ENERGY) buf.append(String.format(Locale.US, " @%.1f", minEnergy / QUANTIZATION));
        return buf.toString();
    }
}
