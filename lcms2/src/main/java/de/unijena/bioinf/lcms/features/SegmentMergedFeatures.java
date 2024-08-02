package de.unijena.bioinf.lcms.features;

import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.ProjectedTrace;
import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.lcms.trace.segmentation.PersistentHomology;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import de.unijena.bioinf.lcms.utils.AlignmentBeamSearch;
import lombok.AllArgsConstructor;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@AllArgsConstructor
public class SegmentMergedFeatures implements MergedFeatureExtractionStrategy {

    @Override
    public TraceSegment[] extractMergedSegments(TraceSegmentationStrategy traceSegmenter, ProcessedSample mergedSample, MergedTrace mergedTrace) {
        SampleStats stats = mergedSample.getStorage().getStatistics();
        return traceSegmenter.detectSegments(stats, mergedTrace).toArray(TraceSegment[]::new);
    }

    @Override
    public TraceSegment[][] extractProjectedSegments(ProcessedSample mergedSample, MergedTrace mergedTrace, TraceSegment[] mergedTraceSegments) {
        TraceSegment[][] rawSegments = new TraceSegment[mergedTrace.getTraces().length][];
        for (int k=0; k < rawSegments.length; ++k) {
            rawSegments[k] = findProjSegment(mergedSample, mergedTrace, mergedTrace.getSamples()[k], mergedTraceSegments, mergedTrace.getTraces()[k]);
        }
        return rawSegments;
    }

    private TraceSegment[] findProjSegment(ProcessedSample mergedSample, MergedTrace mergedTrace, ProcessedSample sample, TraceSegment[] traceSegments, ProjectedTrace trace) {
        final SampleStats stats = sample.getStorage().getStatistics();
        // we have to remap the raw noise level to the projected noise level....
        final double rawNoiseLevel = stats.noiseLevel(trace.getRawApex());
        final double projectedNoiseLevel = trace.projectedIntensity(trace.getProjectedApex()) * rawNoiseLevel / trace.rawIntensity(trace.getRawApex());
        TraceSegment[] childSegments = new PersistentHomology(true).detectSegments(trace.projected(mergedSample.getMapping()), projectedNoiseLevel/10d).toArray(TraceSegment[]::new);
        if (childSegments.length==0) {
            if (traceSegments.length>0) LoggerFactory.getLogger(SegmentMergedFeatures.class).warn("No segments found in child trace!");
            return childSegments;
        }
        // align the child segments with the parent segments
        TraceSegment[] alignedSegments = traceAlignment(mergedSample, mergedTrace, traceSegments, trace.projected(mergedSample.getMapping()), childSegments);
        return alignedSegments;
    }

    private TraceSegment[] traceAlignment(ProcessedSample mergedSample, MergedTrace mergedTrace, TraceSegment[] mergedSegments, Trace projected, TraceSegment[] projectedSegments) {
        double rtDev = mergedSample.getStorage().getAlignmentStorage().getStatistics().getExpectedRetentionTimeDeviation();
        float rtDevPerBin = (float)(rtDev/(mergedSample.getMapping().getRetentionTimeAt(1)-mergedSample.getMapping().getRetentionTimeAt(0)));

        /*
        My feeling is that the average rt error is still too high :/ I have to think about that. For now
        I do the following work around: use a low error, align. Check if average
         */
        double[] segmentIntensities = Arrays.stream(projectedSegments).mapToDouble(x -> projected.intensity(x.apex)).toArray();
        {
            TraceSegment[] matching = new MicroMatching().getMatching(mergedSegments, projectedSegments, rtDevPerBin, Arrays.stream(mergedSegments).mapToDouble(x -> mergedTrace.intensity(x.apex)).toArray(),
                    segmentIntensities, 0d);
            // calculate weighted rt error
            double rtError = 0d, intsum=0d, rtShift = 0d;
            for (int i=0; i < matching.length; ++i) {
                if (matching[i]==null) continue;
                float intens = projected.intensity(matching[i].apex);
                rtError += (mergedSegments[i].apex - matching[i].apex)*(mergedSegments[i].apex - matching[i].apex) * intens;
                rtShift -= (mergedSegments[i].apex - matching[i].apex)*intens;
                intsum += intens;
            }
            if (intsum==0d) return new TraceSegment[mergedSegments.length]; // we haven't found anything...
            double maxIntSum = Arrays.stream(projectedSegments).mapToDouble(x->projected.intensity(x.apex)).sum();
            // if we can assign most of the intensity, we recalibrate the trace and match again
            if (intsum / maxIntSum >= 0.5) {
                rtError /= intsum;
                rtError = Math.sqrt(rtError);
                rtShift /= intsum;
                // repeat the assignment using rtshift as recalibrant
                double rtErrorAfterRecalibration = 0d;
                for (int i = 0; i < matching.length; ++i) {
                    if (matching[i] == null) continue;
                    rtErrorAfterRecalibration += Math.pow(mergedSegments[i].apex - matching[i].apex + rtShift, 2);//*intens;
                }
                rtErrorAfterRecalibration = Math.sqrt(rtErrorAfterRecalibration);

                return new MicroMatching().getMatching(mergedSegments, projectedSegments, (float) Math.min(rtDevPerBin, Math.sqrt(rtErrorAfterRecalibration * rtDevPerBin)), Arrays.stream(mergedSegments).mapToDouble(x -> mergedTrace.intensity(x.apex)).toArray(),
                        segmentIntensities, rtShift);
            } else {
                // match again with normal rtDevPerBin
                return new MicroMatching().getMatching(mergedSegments, projectedSegments, rtDevPerBin, Arrays.stream(mergedSegments).mapToDouble(x -> mergedTrace.intensity(x.apex)).toArray(),
                        segmentIntensities, 0d);
            }

        }
    }

    private interface Matching {
        TraceSegment[] getMatching(TraceSegment[] L, TraceSegment[] R, float averageDistance, double[] intLeft, double[] intRight, double recalibrationRight);
    }

    private static class MicroMatching implements Matching{
        public TraceSegment[] getMatching(TraceSegment[] L, TraceSegment[] R, float averageDistance, double[] intLeft, double[] intRight, double recalibrationRight) {
            norm(intLeft);
            norm(intRight);
            AlignmentBeamSearch.Buffer beamSearch = new AlignmentBeamSearch(5).buffer();
            for (int i=0; i < L.length; ++i) {
                for (int j=0; j < R.length; ++j) {
                    final float distance = (float)Math.abs(L[i].apex - R[j].apex + recalibrationRight);
                    if (distance <= 3*averageDistance) {
                        final float score = (float)(4*Math.exp(-(distance*distance)/averageDistance) + 2*intRight[j] + intLeft[i]*intRight[j]);
                        beamSearch.add(i, j, score);
                    }
                }
            }
            TraceSegment[] merged = new TraceSegment[L.length];
            for (AlignmentBeamSearch.MatchNode match : beamSearch.release().getTopSolution()) {
                merged[match.leftIndex()] = R[match.rightIndex()];
            }
            return merged;
        }
        private static void norm(double[] vec) {
            double n = 0d;
            for (int k=0; k < vec.length; ++k) {
                n += vec[k]*vec[k];
            }
            n = Math.sqrt(n);
            for (int k=0; k < vec.length; ++k) {
                vec[k] /= n;
            }
        }
    }

}
