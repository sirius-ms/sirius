package de.unijena.bioinf.lcms.features;

import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.merge.ScanPointInterpolator;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.ProjectedTrace;
import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.lcms.trace.segmentation.PersistentHomology;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import de.unijena.bioinf.lcms.utils.AlignmentBeamSearch;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;


/*
First, we segment the merged trace, as usual.
Next, we use the features found in the merge trace as poi in the individual traces and segment all of them.
A poi is a scan point that has to be picked - either as its own feature or as part of another features.
Next, we collect all apexes from the individual traces and put them as poi into the merged trace and segment that again.
Finally, we align all of them.

To avoid that we split up a feature into two by accident, we check if two features are close and disjoint. If so,
we merge them again.

This strategy is complicated, but it might be a good compromise between the "merging samples" strategy that just sometimes
don't work very well (in particular for few samples) and the "pick features individually and align them" strategy from mzmine and openms
that just sometimes might work better. Hopefully, this is the best of both worlds!

 */
@AllArgsConstructor
public class SegmentMergedFeatures implements MergedFeatureExtractionStrategy {



    @Override
    public Result featureFinding(TraceSegmentationStrategy traceSegmenter, ProcessedSample mergedSample, MergedTrace mergedTrace) {
        SampleStats stats = mergedSample.getStorage().getStatistics();

        // usually we use the noise level of the merged trace for feature detection. However, if there are MANY samples
        // in the input, the merged trace might become very high-intensive after merging and features that only occur
        // in very few or even single samples might be omitted, even if they have a very high intensity.
        // Thus, we use the minimum of the merged intensity and a single sample noise level multiplied by a high factor
        // to avoid that a lot of noisy peaks are picked
        final double mergedNoiseLevel = stats.noiseLevel(mergedTrace.apex());
        final double FACTOR = 10d;

        double noiseLevel = mergedNoiseLevel;
        for (int k=0; k < mergedTrace.getSamples().length; ++k) {
            final double individualNoiseLevel = mergedTrace.getSamples()[k].getStorage().getStatistics().noiseLevel(mergedTrace.getTraces()[k].getRawApex()) * FACTOR;
            // project individual noise level into merged trace
            final double projectedIndividualNoiseLevel = mergedTrace.getSamples()[k].getNormalizer().normalize(individualNoiseLevel);
            noiseLevel = Math.min(noiseLevel, projectedIndividualNoiseLevel);
        }

        final IntOpenHashSet pointsOfInterest = new IntOpenHashSet(getPointsOfInterest(mergedTrace));

        TraceSegment[] mergedTraceSegments = traceSegmenter.detectSegments(mergedTrace, noiseLevel, stats.getExpectedPeakWidth().orElse(0d), pointsOfInterest.toIntArray()).toArray(TraceSegment[]::new);


        /*
        ENSURE CORRECT ORDERING
         */
        Arrays.sort(mergedTraceSegments, Comparator.comparingInt(x->x.leftEdge));


        /*
        if (mergedTraceSegments.length>=10) {
            try {
                featureFindingDEBUG(traceSegmenter,mergedSample,mergedTrace);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

         */
        /*
        // now extract all subtraces from the individual traces, using the merged apexes as pois
        TraceSegment[][] rawSegments = new TraceSegment[mergedTrace.getTraces().length][];
        for (int k=0; k < rawSegments.length; ++k) {
            rawSegments[k] = extractSegments(mergedSample, mergedTrace, mergedTrace.getSamples()[k], mergedTraceSegments, mergedTrace.getTraces()[k], stats.getExpectedPeakWidth().orElse(0d), false);
            for (TraceSegment seg : rawSegments[k]) {
                pointsOfInterest.add(seg.apex);
            }
        }

        // now redo the merged one with the apexes of the childs as poi
        mergedTraceSegments = traceSegmenter.detectSegments(mergedTrace, noiseLevel, stats.getExpectedPeakWidth().orElse(0d), pointsOfInterest.toIntArray()).toArray(TraceSegment[]::new);
        */
        // and a last time for the child segments...
        TraceSegment[][] rawSegments = new TraceSegment[mergedTrace.getTraces().length][];
        for (int k=0; k < rawSegments.length; ++k) {
            rawSegments[k] = extractSegments(mergedSample, mergedTrace, mergedTrace.getSamples()[k], mergedTraceSegments, mergedTrace.getTraces()[k], stats.getExpectedPeakWidth().orElse(0d), true);
        }

        // finally, merge everything together
        for (int k=0; k < rawSegments.length; ++k) {
            //rawSegments[k] = traceAlignment(mergedSample, mergedTrace, mergedTraceSegments, mergedTrace.getTraces()[k].projected(mergedSample.getMapping()), rawSegments[k]);
            rawSegments[k] = simpleTraceAssignmentByArea(mergedSample, mergedTrace, mergedTraceSegments, mergedTrace.getTraces()[k].projected(mergedSample.getMapping()), rawSegments[k]);
        }

        // join nodes that are disjoint and close
        double rtDev = mergedSample.getStorage().getAlignmentStorage().getStatistics().getExpectedRetentionTimeDeviation();
        Result R = joinClosAndDisjointFeatures(mergedSample, mergedTrace, mergedTraceSegments, rawSegments, rtDev);
        // check if length and everything fits together
        if (mergedTrace.getSamples().length != R.traceSegmentsForIndividualTraces().length)
            throw new RuntimeException("Should not happen!");
        for (TraceSegment[] r : R.traceSegmentsForIndividualTraces()) if (r.length!=R.traceSegmentsForMergedTrace().length) {
            throw new RuntimeException("Should not happen!");
        }
        return R;

    }

    public void featureFindingDEBUG(TraceSegmentationStrategy traceSegmenter, ProcessedSample mergedSample, MergedTrace mergedTrace) throws FileNotFoundException {
        SampleStats stats = mergedSample.getStorage().getStatistics();
        String prefix = String.valueOf((int)(mergedTrace.averagedMz()*1000)) + "_" + String.valueOf((int)(mergedTrace.retentionTime(mergedTrace.apex()))) + "_";
        // usually we use the noise level of the merged trace for feature detection. However, if there are MANY samples
        // in the input, the merged trace might become very high-intensive after merging and features that only occur
        // in very few or even single samples might be omitted, even if they have a very high intensity.
        // Thus, we use the minimum of the merged intensity and a single sample noise level multiplied by a high factor
        // to avoid that a lot of noisy peaks are picked
        final double mergedNoiseLevel = stats.noiseLevel(mergedTrace.apex());
        final double FACTOR = 10d;

        double noiseLevel = mergedNoiseLevel;
        for (int k=0; k < mergedTrace.getSamples().length; ++k) {
            final double individualNoiseLevel = mergedTrace.getSamples()[k].getStorage().getStatistics().noiseLevel(mergedTrace.getTraces()[k].getRawApex()) * FACTOR;
            // project individual noise level into merged trace
            final double projectedIndividualNoiseLevel = mergedTrace.getSamples()[k].getNormalizer().normalize(individualNoiseLevel);
            noiseLevel = Math.min(noiseLevel, projectedIndividualNoiseLevel);
        }

        final IntOpenHashSet pointsOfInterest = new IntOpenHashSet(getPointsOfInterest(mergedTrace));

        TraceSegment[] mergedTraceSegments = traceSegmenter.detectSegments(mergedTrace, noiseLevel, stats.getExpectedPeakWidth().orElse(0d), pointsOfInterest.toIntArray()).toArray(TraceSegment[]::new);

        // now extract all subtraces from the individual traces, using the merged apexes as pois
        TraceSegment[][] rawSegments = new TraceSegment[mergedTrace.getTraces().length][];
        for (int k=0; k < rawSegments.length; ++k) {
            {
                ProjectedTrace trace = mergedTrace.getTraces()[k];
                ProcessedSample sample = mergedTrace.getSamples()[k];
                final SampleStats stats2 = sample.getStorage().getStatistics();
                // we have to remap the raw noise level to the projected noise level....
                final double rawNoiseLevel = stats2.noiseLevel(trace.getRawApex());
                final double projectedNoiseLevel = trace.projectedIntensity(trace.getProjectedApex()) * rawNoiseLevel / trace.rawIntensity(trace.getRawApex());
                int[] pointsOfInterest2 = Arrays.stream(trace.getMs2Refs()).mapToInt(x->sample.getScanPointInterpolator().roundIndex(x.rawScanIdxOfParent)).distinct().toArray();
                // add apexes of parent segments as point of interests?
                final int[] apexesOfParent = Arrays.stream(mergedTraceSegments).filter(Objects::nonNull).mapToInt(x->x.apex).filter(trace::inProjectedRange).toArray();
                int oldn=pointsOfInterest2.length;
                pointsOfInterest2 = Arrays.copyOf(pointsOfInterest2, pointsOfInterest2.length + apexesOfParent.length);
                System.arraycopy(apexesOfParent, 0, pointsOfInterest2, oldn, apexesOfParent.length);
                ////////////////////////////////////////////////////////

                TraceSegment[] childSegments = new PersistentHomology(true).detectSegments(trace.projected(mergedSample.getMapping()),
                        projectedNoiseLevel, stats.getExpectedPeakWidth().orElse(0d), pointsOfInterest2).toArray(TraceSegment[]::new);
                String db = new PersistentHomology(true).debugJson(trace.projected(mergedSample.getMapping()),
                        projectedNoiseLevel, stats.getExpectedPeakWidth().orElse(0d), pointsOfInterest2, apexesOfParent);
                try (final PrintStream out = new PrintStream("/home/kaidu/analysis/debug/many/" + prefix + k +".json")) {
                    out.println(db);
                }
                rawSegments[k]=childSegments;
            }
            for (TraceSegment seg : rawSegments[k]) {
                pointsOfInterest.add(seg.apex);
            }
        }

        // now redo the merged one with the apexes of the childs as poi
        String merged = ((PersistentHomology)traceSegmenter).debugJson(mergedTrace, noiseLevel, stats.getExpectedPeakWidth().orElse(0d), pointsOfInterest.toIntArray());
        try (final PrintStream out = new PrintStream("/home/kaidu/analysis/debug/many/" + prefix + "merged.json")) {
            out.println(merged);
        }

    }

    record PotentialJoinablePair(int i, int j, double distance) {
        public boolean isDisjoint(BitSet[] sampleMappings) {
            BitSet l = (BitSet) sampleMappings[i].clone();
            l.and(sampleMappings[j]);
            return l.isEmpty();
        }
    };

    private Result joinClosAndDisjointFeatures(ProcessedSample merged, MergedTrace mergedTrace, TraceSegment[] mergedTraceSegments, TraceSegment[][] rawSegments, double retentionTimeDeviation) {
        PriorityQueue<PotentialJoinablePair> queue = new PriorityQueue<>(Comparator.comparingDouble(x->x.distance));
        ScanPointMapping mapping = merged.getMapping();
        // sampleMappings[i,j]=1 if feature #i is contained in sample #j
        BitSet[] sampleMappings = new BitSet[mergedTraceSegments.length];
        for (int k=0; k < mergedTraceSegments.length; ++k) {
            sampleMappings[k] = new BitSet(mergedTrace.getSamples().length);
            for (int j=0; j < mergedTrace.getSamples().length; ++j) {
                if (rawSegments[j][k]!=null) sampleMappings[k].set(j);
            }
        }
        for (int k=1; k < mergedTraceSegments.length; ++k) {
            TraceSegment a = mergedTraceSegments[k-1];
            TraceSegment b = mergedTraceSegments[k];
            double distance = (mapping.getRetentionTimeAt(b.apex)-mapping.getRetentionTimeAt(a.apex));
            if (distance <= 3*retentionTimeDeviation)  {
                queue.offer(new PotentialJoinablePair(k-1, k, distance));
            }
        }
        BitSet deleted = new BitSet(mergedTraceSegments.length);
        while (!queue.isEmpty()) {
            PotentialJoinablePair pair = queue.poll();
            if (deleted.get(pair.i) || deleted.get(pair.j)) continue;
            if (pair.isDisjoint(sampleMappings)) {
                // merge pair!
                TraceSegment a = mergedTraceSegments[pair.i];
                TraceSegment b = mergedTraceSegments[pair.j];
                if (mergedTrace.intensity(a.apex)>mergedTrace.intensity(b.apex)) {
                    a.rightEdge = b.rightEdge;
                    deleted.set(pair.j);
                    sampleMappings[pair.i].or(sampleMappings[pair.j]);
                    for (int l=sampleMappings[pair.j].nextSetBit(0); l>=0; l = sampleMappings[pair.j].nextSetBit(l+1)) {
                        rawSegments[l][pair.i]=rawSegments[l][pair.j];
                        if (rawSegments[l][pair.i]==null) throw new RuntimeException("SHOULD NOT HAPPEN");
                    }
                    for (int k=pair.i+1; k < mergedTraceSegments.length; ++k) {
                        if (deleted.get(k)) continue;
                        TraceSegment c = mergedTraceSegments[k];
                        double distance = (mapping.getRetentionTimeAt(c.apex)-mapping.getRetentionTimeAt(a.apex));
                        if (distance <= 3*retentionTimeDeviation)  {
                            queue.offer(new PotentialJoinablePair(pair.i, k, distance));
                        }
                        break;
                    }
                } else {
                    b.leftEdge = a.leftEdge;
                    deleted.set(pair.i);
                    sampleMappings[pair.j].or(sampleMappings[pair.i]);
                    for (int l=sampleMappings[pair.i].nextSetBit(0); l>=0; l = sampleMappings[pair.i].nextSetBit(l+1)) {
                        rawSegments[l][pair.j]=rawSegments[l][pair.i];
                        if (rawSegments[l][pair.j]==null) throw new RuntimeException("SHOULD NOT HAPPEN");
                    }
                    for (int k=pair.j-1; k >= 0; --k) {
                        if (deleted.get(k)) continue;
                        TraceSegment c = mergedTraceSegments[k];
                        double distance = (mapping.getRetentionTimeAt(b.apex)-mapping.getRetentionTimeAt(c.apex));
                        if (distance <= 3*retentionTimeDeviation)  {
                            queue.offer(new PotentialJoinablePair(k, pair.j, distance));
                        }
                        break;
                    }
                }
            }
        }
        ArrayList<TraceSegment> resultMerged = new ArrayList<>();
        ArrayList<TraceSegment>[] resultsRaw = new ArrayList[mergedTrace.getSamples().length];
        for (int k=0; k < resultsRaw.length; ++k) resultsRaw[k]=new ArrayList<>();
        for (int k=0;k < mergedTraceSegments.length; ++k) {
            if (!deleted.get(k)) {
                resultMerged.add(mergedTraceSegments[k]);
                for (int l=0; l < mergedTrace.getSamples().length; ++l) resultsRaw[l].add(rawSegments[l][k]);
            }
        }


        return new Result(resultMerged.toArray(TraceSegment[]::new), Arrays.stream(resultsRaw).map(x->x.toArray(TraceSegment[]::new)).toArray(TraceSegment[][]::new));
    }


    public TraceSegment[] extractSegments(ProcessedSample mergedSample, MergedTrace mergedTrace, ProcessedSample sample, TraceSegment[] traceSegments, ProjectedTrace trace, double expectedPeakWidth, boolean enforceFeatures) {
        final SampleStats stats = sample.getStorage().getStatistics();
        // we have to remap the raw noise level to the projected noise level....
        final double rawNoiseLevel = stats.noiseLevel(trace.getRawApex());
        final double projectedNoiseLevel = trace.projectedIntensity(trace.getProjectedApex()) * rawNoiseLevel / trace.rawIntensity(trace.getRawApex());
        int[] pointsOfInterest = Arrays.stream(trace.getMs2Refs()).mapToInt(x->sample.getScanPointInterpolator().roundIndex(x.rawScanIdxOfParent)).distinct().toArray();
        // add apexes of parent segments as point of interests?
        final int[] apexesOfParent = Arrays.stream(traceSegments).filter(Objects::nonNull).mapToInt(x->x.apex).filter(trace::inProjectedRange).toArray();
        int oldn=pointsOfInterest.length;
        pointsOfInterest = Arrays.copyOf(pointsOfInterest, pointsOfInterest.length + apexesOfParent.length);
        System.arraycopy(apexesOfParent, 0, pointsOfInterest, oldn, apexesOfParent.length);
        ////////////////////////////////////////////////////////
        int[] features;
        if (enforceFeatures) {
            features = Arrays.stream(traceSegments).mapToInt(x->x.apex).toArray();
        } else {
            features = new int[0];
        }

        TraceSegment[] childSegments = new PersistentHomology(true).detectSegments(trace.projected(mergedSample.getMapping()),
                projectedNoiseLevel, expectedPeakWidth, pointsOfInterest, features).toArray(TraceSegment[]::new);
        return childSegments;
    }

    private static @NotNull int[] getPointsOfInterest(MergedTrace mergedTrace) {
        final IntOpenHashSet pointsOfInterest = new IntOpenHashSet();
        for (int i = 0; i < mergedTrace.getTraces().length; ++i)  {
            ProjectedTrace t = mergedTrace.getTraces()[i];
            ScanPointInterpolator interpolator = mergedTrace.getSamples()[i].getScanPointInterpolator();
            Arrays.stream(t.getMs2Refs()).mapToInt(x->interpolator.roundIndex(x.rawScanIdxOfParent)).forEach(pointsOfInterest::add);
        }
        return pointsOfInterest.toIntArray();
    }

    private TraceSegment[] simpleTraceAssignment(ProcessedSample mergedSample, MergedTrace mergedTrace, TraceSegment[] mergedSegments, Trace projected, TraceSegment[] projectedSegments) {
        TraceSegment[] matching = new TraceSegment[mergedSegments.length];
        double rtDev = mergedSample.getStorage().getAlignmentStorage().getStatistics().getExpectedRetentionTimeDeviation();
        float rtDevPerBin = (float)(rtDev/(mergedSample.getMapping().getRetentionTimeAt(1)-mergedSample.getMapping().getRetentionTimeAt(0)));
        List<TraceSegment>[] segments = new ArrayList[mergedSegments.length];
        for (int i=0; i < segments.length; ++i) segments[i] = new ArrayList<>();
        for (int i=0; i < projectedSegments.length; ++i) {
            int bestFittingSegment = -1;
            float bestFittingDistance = Float.POSITIVE_INFINITY;
            for (int j=0; j < mergedSegments.length; ++j) {
                if (projectedSegments[i].leftEdge <= mergedSegments[j].apex+rtDevPerBin && projectedSegments[i].rightEdge >= mergedSegments[j].apex-rtDevPerBin) {
                    //if (mergedSegments[j].leftEdge <= apex+rtDevPerBin && mergedSegments[j].rightEdge >= apex-rtDevPerBin) {
                    float dist = Math.abs(mergedSegments[j].apex - projectedSegments[i].apex);
                    if (dist < bestFittingDistance) {
                        bestFittingDistance = dist;
                        bestFittingSegment = j;
                    }
                }
            }
            if (bestFittingSegment>=0) {
                segments[bestFittingSegment].add(projectedSegments[i]);
            }
        }
        // now merge everything together
        return Arrays.stream(segments).map(segs->{
            if (segs.size()==0) return null;
            if (segs.size()==1) return segs.get(0);
            segs.sort(Comparator.comparingInt(x->x.apex));
            int left = segs.get(0).leftEdge;
            int right = segs.get(segs.size()-1).rightEdge;
            segs.sort(Comparator.comparingDouble(x->-projected.intensity(x.apex)));
            int apex = segs.get(0).apex;
            return new TraceSegment(apex, left, right);
        }).toArray(TraceSegment[]::new);
    }
    private TraceSegment[] simpleTraceAssignmentByArea(ProcessedSample mergedSample, MergedTrace mergedTrace, TraceSegment[] mergedSegments, Trace projected, TraceSegment[] projectedSegments) {
        TraceSegment[] matching = new TraceSegment[mergedSegments.length];
        double rtDev = mergedSample.getStorage().getAlignmentStorage().getStatistics().getExpectedRetentionTimeDeviation();
        float rtDevPerBin = (float)(rtDev/(mergedSample.getMapping().getRetentionTimeAt(1)-mergedSample.getMapping().getRetentionTimeAt(0)));
        List<TraceSegment>[] segments = new ArrayList[mergedSegments.length];
        for (int i=0; i < segments.length; ++i) segments[i] = new ArrayList<>();
        for (int i=0; i < projectedSegments.length; ++i) {
            int bestFittingSegment = -1;
            double bestFittingDistance = 0d;
            for (int j=0; j < mergedSegments.length; ++j) {
                final boolean mergedInProjected = projectedSegments[i].leftEdge <= mergedSegments[j].apex+rtDevPerBin && projectedSegments[i].rightEdge >= mergedSegments[j].apex-rtDevPerBin;
                final boolean projectedInMerged = mergedSegments[j].leftEdge <= projectedSegments[i].apex+rtDevPerBin && mergedSegments[j].rightEdge >= projectedSegments[i].apex-rtDevPerBin;
                if (!(mergedInProjected || projectedInMerged)) continue;
                double overlappingArea = 0;
                for (int k=Math.max(projectedSegments[i].leftEdge, mergedSegments[j].leftEdge); k <= Math.min(projectedSegments[i].rightEdge, mergedSegments[j].rightEdge); ++k) {
                    overlappingArea += projected.intensityOrZero(k)*mergedTrace.intensityOrZero(k);
                }
                if (overlappingArea>bestFittingDistance) {
                    bestFittingDistance = overlappingArea;
                    bestFittingSegment = j;
                }
            }
            if (bestFittingSegment>=0) {
                segments[bestFittingSegment].add(projectedSegments[i]);
            }
        }
        // now merge everything together
        return Arrays.stream(segments).map(segs->{
            if (segs.size()==0) return null;
            if (segs.size()==1) return segs.get(0);
            segs.sort(Comparator.comparingInt(x->x.apex));
            int left = segs.get(0).leftEdge;
            int right = segs.get(segs.size()-1).rightEdge;
            segs.sort(Comparator.comparingDouble(x->-projected.intensity(x.apex)));
            int apex = segs.get(0).apex;
            return new TraceSegment(apex, left, right);
        }).toArray(TraceSegment[]::new);
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
            TraceSegment[] matching = new MicroMatching().getMatching(mergedSegments, projectedSegments, rtDevPerBin, Arrays.stream(mergedSegments).mapToDouble(x -> x==null?0d:mergedTrace.intensity(x.apex)).toArray(),
                    segmentIntensities, 0d);
            if (matching.length>=5) {
                // calculate weighted rt error
                double rtError = 0d, intsum = 0d, rtShift = 0d;
                for (int i = 0; i < matching.length; ++i) {
                    if (matching[i] == null) continue;
                    float intens = projected.intensity(matching[i].apex);
                    rtError += (mergedSegments[i].apex - matching[i].apex) * (mergedSegments[i].apex - matching[i].apex) * intens;
                    rtShift -= (mergedSegments[i].apex - matching[i].apex) * intens;
                    intsum += intens;
                }
                if (intsum == 0d) return new TraceSegment[mergedSegments.length]; // we haven't found anything...
                double maxIntSum = Arrays.stream(projectedSegments).mapToDouble(x -> projected.intensity(x.apex)).sum();
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

                    return new MicroMatching().getMatching(mergedSegments, projectedSegments, (float) Math.min(rtDevPerBin, Math.sqrt(rtErrorAfterRecalibration * rtDevPerBin)), Arrays.stream(mergedSegments).mapToDouble(x -> x==null ? 0d : mergedTrace.intensity(x.apex)).toArray(),
                            segmentIntensities, rtShift);
                } else return matching;
            } else return matching;

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
                if (L[i]==null) continue;
                for (int j=0; j < R.length; ++j) {
                    if (R[j]==null) continue;
                    final float distance = (float)Math.abs(L[i].apex - R[j].apex + recalibrationRight);
                    if (distance <= 4*averageDistance /*|| R[j].overlaps(L[i])*/) {
                        float overlap = ((Math.min(L[i].rightEdge, R[j].rightEdge)) - Math.max(L[i].leftEdge,R[j].leftEdge))/(float)Math.min(L[i].rightEdge-L[i].leftEdge,R[j].rightEdge-R[j].leftEdge);
                        final float score = (float)(6*Math.exp(-(distance*distance)/(2*averageDistance*averageDistance)) + 4*overlap + 2*intRight[j] + intLeft[i]*intRight[j]) - (float)Math.sqrt(distance/averageDistance);
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
