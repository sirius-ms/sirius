package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.features.MergedFeatureExtractionStrategy;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.msms.MsMsTraceReference;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.ProjectedTrace;
import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * The precursor features of one merged trace as feature extraction will finally see them, per sample.
 * <p>
 * Stage one of the DIA assignment had to work with the preliminary segments of trace picking, because the real
 * feature boundaries are a consensus over the aligned samples and do not exist before alignment. This is where they
 * do: {@link MergedFeatureExtractionStrategy#featureFinding} is pure computation on the merged sample, so the same
 * boundaries the importer will use can be had before anything is imported, and the assignment can be redone over
 * them without a way of writing corrections back into a project.
 * <p>
 * Two things are collected per sample and feature. The <b>window</b> in that sample's own MS1 scan index space,
 * which is what a correlation against that sample's fragments has to be measured over, computed exactly as
 * {@code PickFeaturesAndImportToSirius.findFeaturesForTraces} computes the {@code RawTraceRef} of a sub feature.
 * And the <b>pseudo spectra</b> that will end up in this feature, because stage two can only revise the spectra
 * stage one wrote: their references were copied into the projected traces during merging and are no longer read
 * from the trace storage, so a spectrum added now would never be seen, while the peaks of an existing one are read
 * back at import time and can be replaced.
 * <p>
 * Which spectrum belongs to which feature is decided by the rule of {@code MergeGreedyStrategy.assignMs2ToSegments}
 * - the segment closest in recalibrated retention time, with the merged segments as fallback, and no distance limit
 * - because that is the rule that will actually be applied afterwards. Getting this wrong would not lose a
 * spectrum, it would attach a corrected spectrum to the wrong feature, which is worse.
 */
public final class DiaFinalPrecursors {

    /**
     * A feature as seen in one sample: the window to measure over, and the pseudo spectra that will carry the
     * result. Both are in that sample's own index space.
     */
    @Getter
    public static final class Instance {
        private final ProcessedSample sample;
        /** the sample's own trace, in raw scan index space */
        private final Trace rawTrace;
        /** the final feature boundaries, in that sample's raw scan indices */
        private final TraceSegment rawWindow;
        private final double mz;
        private final float apexIntensity;
        /** uids of the pseudo spectra that feature extraction will collect for this feature */
        private final int[] pseudoSpectra;

        static Instance of(ProcessedSample sample, Trace rawTrace, TraceSegment rawWindow, double mz,
                           float apexIntensity, int[] pseudoSpectra) {
            return new Instance(sample, rawTrace, rawWindow, mz, apexIntensity, pseudoSpectra);
        }

        private Instance(ProcessedSample sample, Trace rawTrace, TraceSegment rawWindow, double mz,
                         float apexIntensity, int[] pseudoSpectra) {
            this.sample = sample;
            this.rawTrace = rawTrace;
            this.rawWindow = rawWindow;
            this.mz = mz;
            this.apexIntensity = apexIntensity;
            this.pseudoSpectra = pseudoSpectra;
        }
    }

    /**
     * One feature of the merged trace, across the samples that contributed to it. This is the node a fragment gets
     * assigned to in stage two: the same compound in three samples is one decision, not three.
     */
    @Getter
    public static final class Aligned {
        private final double mz;
        private final double retentionTime;
        private final List<Instance> instances;

        static Aligned of(double mz, double retentionTime, List<Instance> instances) {
            return new Aligned(mz, retentionTime, instances);
        }

        private Aligned(double mz, double retentionTime, List<Instance> instances) {
            this.mz = mz;
            this.retentionTime = retentionTime;
            this.instances = instances;
        }
    }

    private DiaFinalPrecursors() {
    }

    /**
     * @param pickedFeatures the final segmentation of this merged trace, from
     *                       {@link MergedFeatureExtractionStrategy#featureFinding}
     * @return one entry per feature of the merged trace that at least one sample contributes to
     */
    public static List<Aligned> of(ProcessedSample mergedSample, MergedTrace mergedTrace,
                                   MergedFeatureExtractionStrategy.Result pickedFeatures) {
        final TraceSegment[] mergedSegments = pickedFeatures.traceSegmentsForMergedTrace();
        final TraceSegment[][] segmentsPerSample = pickedFeatures.traceSegmentsForIndividualTraces();
        final List<Aligned> features = new ArrayList<>(mergedSegments.length);
        // which pseudo spectrum ends up in which feature, per sample
        final int[][][] spectraPerSample = new int[segmentsPerSample.length][][];
        for (int k = 0; k < segmentsPerSample.length; ++k) {
            spectraPerSample[k] = assignSpectraToSegments(mergedSample, mergedTrace.getSamples()[k],
                    mergedTrace.getTraces()[k], segmentsPerSample[k], mergedSegments);
        }

        for (int fid = 0; fid < mergedSegments.length; ++fid) {
            if (mergedSegments[fid] == null) continue;
            final List<Instance> instances = new ArrayList<>(segmentsPerSample.length);
            for (int k = 0; k < segmentsPerSample.length; ++k) {
                final TraceSegment segment = segmentsPerSample[k][fid];
                // the same filter the importer applies before it makes a sub feature of a segment
                if (segment == null || segment.rightEdge - segment.leftEdge <= 2) continue;
                final ProcessedSample sample = mergedTrace.getSamples()[k];
                final ProjectedTrace projected = mergedTrace.getTraces()[k];
                final Trace raw = projected.raw(sample.getMapping());
                final TraceSegment rawWindow = rawWindowOf(projected, sample, segment, raw);
                instances.add(Instance.of(sample, raw, rawWindow, projected.getAveragedMz(),
                        raw.intensity(rawWindow.apex), spectraPerSample[k][fid]));
            }
            if (instances.isEmpty()) continue;
            features.add(Aligned.of(mergedTrace.averagedMz(),
                    mergedTrace.retentionTime(mergedSegments[fid].apex), List.copyOf(instances)));
        }
        return features;
    }

    /**
     * Translates a feature of the merged trace into the sample's own scan indices, clamped to the part of the trace
     * that sample actually measured.
     */
    static TraceSegment rawWindowOf(ProjectedTrace projected, ProcessedSample sample, TraceSegment mergedSegment,
                                    Trace raw) {
        final int from = Math.max(projected.getRawStartId(),
                sample.getScanPointInterpolator().reverseMapLowerIndex(mergedSegment.leftEdge));
        final int to = Math.min(projected.getRawEndId(),
                sample.getScanPointInterpolator().reverseMapLargerIndex(mergedSegment.rightEdge));
        // the apex of the merged trace need not be where this sample peaks, so it is taken from the sample itself
        int apex = from;
        double best = -1d;
        for (int k = from; k <= to; ++k) {
            final double intensity = raw.intensity(k);
            if (intensity > best) {
                best = intensity;
                apex = k;
            }
        }
        return new TraceSegment(apex, from, Math.max(from, to));
    }

    /**
     * Applies the rule of {@code MergeGreedyStrategy.assignMs2ToSegments}: every spectrum goes to the segment
     * closest to it in recalibrated retention time, the segments of the merged trace serving as a fallback where
     * this sample has no segment of its own. There is deliberately no distance limit, so every spectrum of the
     * trace lands somewhere.
     *
     * @return the pseudo spectra per feature index of this sample
     */
    static int[][] assignSpectraToSegments(ProcessedSample mergedSample, ProcessedSample sample,
                                           ProjectedTrace projected, TraceSegment[] segments,
                                           TraceSegment[] mergedSegments) {
        final IntArrayList[] perSegment = new IntArrayList[mergedSegments.length];
        for (int k = 0; k < perSegment.length; ++k) perSegment[k] = new IntArrayList();
        final MsMsTraceReference[] references = projected.getMs2Refs();
        if (references != null) {
            for (MsMsTraceReference reference : references) {
                final Ms2SpectrumHeader header =
                        sample.getStorage().getSpectrumStorage().ms2SpectrumHeader(reference.ms2Uid);
                if (header == null || !DiaPseudoSpectra.isDeconvoluted(header)) continue;
                final double rt = sample.getRtRecalibration().value(header.getRetentionTime());
                int assignment = closestSegment(mergedSample, segments, rt);
                if (assignment < 0) assignment = closestSegment(mergedSample, mergedSegments, rt);
                if (assignment >= 0) perSegment[assignment].add(reference.ms2Uid);
            }
        }
        final int[][] result = new int[perSegment.length][];
        for (int k = 0; k < perSegment.length; ++k) result[k] = perSegment[k].toIntArray();
        return result;
    }

    /** @return index of the segment containing or closest to the retention time, or -1 if there is no segment */
    private static int closestSegment(ProcessedSample mergedSample, TraceSegment[] segments, double retentionTime) {
        int best = -1;
        double bestDistance = Double.POSITIVE_INFINITY;
        if (segments == null) return -1;
        for (int k = 0; k < segments.length; ++k) {
            if (segments[k] == null) continue;
            final double left = mergedSample.getMapping().getRetentionTimeAt(segments[k].leftEdge);
            final double right = mergedSample.getMapping().getRetentionTimeAt(segments[k].rightEdge);
            final double distance = retentionTime < left ? left - retentionTime
                    : retentionTime > right ? retentionTime - right : 0d;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = k;
            }
            if (distance == 0d) break;
        }
        return best;
    }
}
