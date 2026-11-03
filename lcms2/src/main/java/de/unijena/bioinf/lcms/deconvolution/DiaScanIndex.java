package de.unijena.bioinf.lcms.deconvolution;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.List;
import java.util.Locale;

/**
 * The DIA acquisition scheme of one sample: which m/z ranges were fragmented, how often, and when.
 * <p>
 * This is the bridge between the MS1 world and the fragment world. Everything downstream asks it exactly one
 * question - "which channels could have fragmented a precursor at this m/z?" - and then works within those
 * channels.
 */
public class DiaScanIndex {

    /** channels in a canonical order (ascending in their lowest isolated m/z) */
    @Getter
    private final List<DiaChannel> channels;

    /**
     * number of MS2 scans that were not assigned to any usable channel, either because they carried no
     * isolation window or because their channel fired too rarely to carry a peak shape
     */
    @Getter
    private final int numberOfDroppedScans;

    /** number of scans for which we could not trust the parent MS1 reference and fell back to retention time */
    @Getter
    private final int numberOfRepairedParentReferences;

    /**
     * spectra that were computed by the deconvolution rather than acquired, and therefore not part of the
     * acquisition scheme
     */
    @Getter
    private final int numberOfDeconvolutedScans;

    public DiaScanIndex(List<DiaChannel> channels, int numberOfDroppedScans, int numberOfRepairedParentReferences) {
        this(channels, numberOfDroppedScans, numberOfRepairedParentReferences, 0);
    }

    public DiaScanIndex(List<DiaChannel> channels, int numberOfDroppedScans, int numberOfRepairedParentReferences,
                        int numberOfDeconvolutedScans) {
        this.channels = Collections.unmodifiableList(channels);
        this.numberOfDroppedScans = numberOfDroppedScans;
        this.numberOfRepairedParentReferences = numberOfRepairedParentReferences;
        this.numberOfDeconvolutedScans = numberOfDeconvolutedScans;
    }

    /**
     * The MS2 scans its channels took, so that everything else can go to the data dependent path.
     * <p>
     * A sample is not DIA or DDA as a whole: whether a window's scans can be deconvoluted depends on how densely it
     * was revisited, and an acquisition may hold windows of both kinds. The scans this index did not claim are
     * ordinary MS2 measurements of a single precursor and have to be assigned as such, or they are lost - which is
     * what happened to eighty five per cent of the scans of a data dependent run whose windows were merely wide.
     */
    public IntSet claimedMs2Uids() {
        final IntSet claimed = new IntOpenHashSet();
        for (DiaChannel channel : channels) {
            for (int k = 0; k < channel.numberOfScans(); ++k) claimed.add(channel.ms2UidAt(k));
        }
        return claimed;
    }

    public int numberOfChannels() {
        return channels.size();
    }

    /**
     * @return all channels whose isolation windows contain the given precursor m/z. More than one channel can
     * match if the acquisition scheme uses overlapping windows or several collision energies.
     */
    public List<DiaChannel> channelsCovering(double precursorMz) {
        final List<DiaChannel> hits = new ArrayList<>(2);
        for (DiaChannel channel : channels) {
            if (channel.covers(precursorMz)) hits.add(channel);
        }
        return hits;
    }

    /**
     * @return total number of MS2 scans covered by this index
     */
    public int numberOfScans() {
        int sum = 0;
        for (DiaChannel channel : channels) sum += channel.numberOfScans();
        return sum;
    }

    /**
     * Human readable summary. This is the first thing to look at when a DIA file behaves strangely, so it
     * deliberately reports the numbers that tell us whether the acquisition scheme was understood correctly.
     */
    public String describe() {
        StringBuilder buf = new StringBuilder();
        buf.append(String.format(Locale.US, "DIA scan index: %d channels, %d MS2 scans", numberOfChannels(), numberOfScans()));
        if (numberOfDroppedScans > 0) buf.append(String.format(Locale.US, ", %d scans dropped", numberOfDroppedScans));
        if (numberOfRepairedParentReferences > 0)
            buf.append(String.format(Locale.US, ", %d parent references repaired", numberOfRepairedParentReferences));
        if (numberOfDeconvolutedScans > 0)
            buf.append(String.format(Locale.US, ", %d deconvoluted spectra ignored", numberOfDeconvolutedScans));
        buf.append('\n');
        for (DiaChannel channel : channels) {
            buf.append(String.format(Locale.US, "  %s, every %.2f s%n", channel, channel.medianScanInterval()));
        }
        // the covered m/z range is a quick sanity check that we did not misread the acquisition scheme
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
        for (DiaChannel channel : channels) {
            min = Math.min(min, channel.lowestMz());
            max = Math.max(max, channel.highestMz());
        }
        if (!channels.isEmpty()) {
            buf.append(String.format(Locale.US, "  m/z coverage: %.1f - %.1f%n", min, max));
        }
        return buf.toString();
    }

    @Override
    public String toString() {
        return "DiaScanIndex[" + numberOfChannels() + " channels, " + numberOfScans() + " scans]";
    }
}
