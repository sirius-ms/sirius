package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.trace.Trace;

/**
 * How much of a fragment's signal in its channel falls inside one precursor feature.
 * <p>
 * This is the signal that survives when there is no shape to correlate. A channel samples a peak only four to
 * nine times, so a fragment the channel caught once or twice carries no elution profile - but if all of that
 * fragment's signal in the whole run sits inside this one feature, that is still evidence, and it is computable
 * from a single scan point.
 * <p>
 * The limit is worth stating: specificity rejects background masses but cannot separate co-eluting candidates. A
 * fragment measured in two scans is equally specific to every precursor whose window contains those two scans.
 * Splitting those requires either an elution profile or, after alignment, abundance across samples.
 */
public final class DiaFragmentSpecificity {

    private DiaFragmentSpecificity() {
    }

    /**
     * @param fragment a trace on the axis of the channel
     * @param window   the scan points covering one precursor feature
     * @return the fraction of the fragment's summed intensity that lies inside the window, between zero and one.
     * One means the fragment was only ever seen within this feature, a small value means it is present across
     * much of the run and is therefore no evidence for this precursor in particular.
     */
    public static double of(Trace fragment, ChannelWindow window) {
        double inside = 0d, total = 0d;
        for (int k = fragment.startId(); k <= fragment.endId(); ++k) {
            final double intensity = fragment.intensity(k);
            if (intensity <= 0) continue;
            total += intensity;
            if (window.contains(k)) inside += intensity;
        }
        if (total <= 0) return 0d;
        return inside / total;
    }
}
