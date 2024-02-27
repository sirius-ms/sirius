package de.unijena.bioinf.lcms.features;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.msms.MergedSpectrum;
import de.unijena.bioinf.lcms.msms.Ms2MergeStrategy;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.lcms.trace.segmentation.PersistentHomology;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.ms.persistence.model.core.IsotopePattern;
import de.unijena.bioinf.ms.persistence.model.core.feature.*;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MergedFeatureExtractor implements MergedFeatureExtractionStrategy{

    // TODO stats
    // TODO intensity normalization
    // FIXME mz in aligned features

    @Override
    public Iterator<AlignedFeatures> extractFeatures(ProcessedSample mergedSample, MergedTrace mergedTrace, Ms2MergeStrategy ms2MergeStrategy, IsotopePatternExtractionStrategy isotopePatternExtractionStrategy, Int2LongMap trace2trace, Int2ObjectMap<ProcessedSample> idx2sample) {
        ProcessedSample[] samplesInTrace = new ProcessedSample[mergedTrace.getSampleIds().size()];
        for (int i = 0; i < mergedTrace.getSampleIds().size(); ++i) {
            samplesInTrace[i] = idx2sample.get(mergedTrace.getSampleIds().getInt(i));
        }

        final Trace mTrace = mergedTrace.toTrace(mergedSample);
        final SampleStats stats = mergedSample.getStorage().getStatistics();
        TraceSegment[] traceSegments = new PersistentHomology().detectSegments(mergedSample.getStorage().getStatistics(), mTrace).stream().filter(x->mTrace.intensity(x.apex) > stats.noiseLevel(x.apex)  && x.leftEdge < x.rightEdge - 1).toArray(TraceSegment[]::new);
        if (traceSegments.length == 0)
            return Collections.emptyIterator();

        Arrays.sort(traceSegments, Comparator.comparingDouble(a -> a.apex));
        double[][] intervals = new double[traceSegments.length][2];
        for (int i = 0; i < traceSegments.length; i++) {
            intervals[i][0] = mTrace.retentionTime(traceSegments[i].leftEdge);
            intervals[i][1] = mTrace.retentionTime(traceSegments[i].rightEdge);
        }

        AlignedFeatures[] monoisotopic = extractAlignedFeatures(traceSegments, mergedSample, samplesInTrace, mergedTrace, ms2MergeStrategy, trace2trace, AlignedFeatures::new, AlignedFeatures[]::new);

        if (!mergedTrace.getIsotopeUids().isEmpty()) {
            AlignedIsotopicFeatures[][] isotopicFeatures = new AlignedIsotopicFeatures[monoisotopic.length][mergedTrace.getIsotopeUids().size()];
            for (int j = 0; j < mergedTrace.getIsotopeUids().size(); j++) {
                MergedTrace isoTrace = mergedSample.getStorage().getMergeStorage().getMerged(mergedTrace.getIsotopeUids().getInt(j));
                ProcessedSample[] isoSamplesInTrace = new ProcessedSample[isoTrace.getSampleIds().size()];
                for (int k = 0; k < isoTrace.getSampleIds().size(); ++k) {
                    isoSamplesInTrace[k] = idx2sample.get(isoTrace.getSampleIds().getInt(k));
                }

                TraceSegment[] isoTraceSegments = assignIntervalstoTrace(mergedSample, isoTrace, intervals);
                AlignedIsotopicFeatures[] isoFeatures = extractAlignedFeatures(isoTraceSegments, mergedSample, isoSamplesInTrace, isoTrace, ms2MergeStrategy, trace2trace, AlignedIsotopicFeatures::new, AlignedIsotopicFeatures[]::new);
                for (int i = 0; i < monoisotopic.length; i++) {
                    isotopicFeatures[i][j] = isoFeatures[i];
                }
            }

            for (int i = 0; i < monoisotopic.length; i++) {
                if (monoisotopic[i] == null)
                    continue;
                List<AlignedIsotopicFeatures> isotopicFeaturesList = new ArrayList<>();
                for (int j = 0; j < isotopicFeatures[i].length; j++) {
                    if (isotopicFeatures[i][j] == null)
                        break;
                    isotopicFeaturesList.add(isotopicFeatures[i][j]);
                }
                monoisotopic[i].setIsotopicFeatures(isotopicFeaturesList);
                monoisotopic[i].setIsotopePattern(isotopePatternExtractionStrategy.extractIsotopePattern(monoisotopic[i], isotopicFeaturesList));
            }
        }
        return Arrays.stream(monoisotopic).filter(Objects::nonNull).iterator();
    }

    private <F extends AbstractAlignedFeatures> F[] extractAlignedFeatures(
            TraceSegment[] traceSegments, ProcessedSample mergedSample, ProcessedSample[] samplesInTrace, MergedTrace mergedTrace, Ms2MergeStrategy ms2MergeStrategy, Int2LongMap trace2trace, Supplier<F> featureSupplier, IntFunction<F[]> featureArraySupplier
    ) {
        Trace mTrace = mergedTrace.toTrace(mergedSample);

        // segments for each individual trace
        TraceSegment[][] individualSegments = new TraceSegment[mergedTrace.getSampleIds().size()][];
        for (int k=0; k < individualSegments.length; ++k) {
            ProcessedSample sample = samplesInTrace[k];
            assert sample.getUid() == mergedTrace.getSampleIds().getInt(k);
            individualSegments[k] = assignSegmentsToIndividualTrace(sample, traceSegments, mergedSample.getStorage().getMergeStorage().getTrace(mergedTrace.getTraceIds().getInt(k)) );
        }

        final Int2ObjectOpenHashMap<ProcessedSample> uid2sample = new Int2ObjectOpenHashMap<>();
        for (ProcessedSample sample : samplesInTrace) {
            uid2sample.put(sample.getUid(), sample);
        }
        {
            ms2MergeStrategy.assignMs2(mergedSample, mergedTrace, traceSegments, uid2sample, new Ms2MergeStrategy.AssignMs2ToFeature() {
                @Override
                public void assignMs2ToFeature(MergedTrace mergedTrace, TraceSegment segment, MergedSpectrum mergedSpectrum) {
                    // TODO: Do something with MS/MS
                }
            });
        }

        F[] featureArr = featureArraySupplier.apply(traceSegments.length);

        for (int i = 0; i < traceSegments.length; i++) {
            if (traceSegments[i] == null)
                continue;

            F alignedFeatures = buildFeature(mergedTrace.getUid(), mTrace, traceSegments[i], trace2trace, featureSupplier.get());
            List<Feature> childFeatures = new ArrayList<>();
            for (int k=0; k < individualSegments.length; ++k) {
                if (individualSegments[k][i] == null)
                    continue;

                int childTraceId = mergedTrace.getTraceIds().getInt(k);
                Feature feature = buildFeature(childTraceId, mergedSample.getStorage().getMergeStorage().getTrace(childTraceId), individualSegments[k][i], trace2trace, Feature.builder().build());
                childFeatures.add(feature);
            }
            if (childFeatures.isEmpty())
                continue;

            alignedFeatures.setFeatures(childFeatures);
            featureArr[i] = alignedFeatures;
        }

        return featureArr;
    }

    private <F extends AbstractFeature> F buildFeature (
            int traceUid,
            Trace mTrace,
            TraceSegment segment,
            Int2LongMap trace2trace,
            F feature
    ) {
        final int o = mTrace.startId();

        RetentionTime rt = new RetentionTime(
                mTrace.retentionTime(segment.leftEdge - o),
                mTrace.retentionTime(segment.rightEdge - o),
                mTrace.retentionTime(segment.apex - o)
        );

        feature.setRetentionTime(rt);
        feature.setApexMass(mTrace.mz(segment.apex));
        feature.setApexIntensity(mTrace.intensity(segment.apex));
        feature.setAverageMass(mTrace.averagedMz());

        if (!trace2trace.containsKey(traceUid)) {
            throw new RuntimeException(String.format("Unknown trace with uid %d", traceUid));
        }

        feature.setTraceRef(new TraceRef(trace2trace.get(traceUid), segment.leftEdge - o, segment.apex - o, segment.rightEdge -o));

        return feature;
    }

    public String extractFeaturesToString(ProcessedSample mergedSample, ProcessedSample[] samplesInTrace, MergedTrace alignedFeature, Ms2MergeStrategy ms2MergeStrategy) {
        // segments for merged trace
        Trace mergedTrace = alignedFeature.toTrace(mergedSample);
        SampleStats stats = mergedSample.getStorage().getStatistics();
        TraceSegment[] traceSegments = new PersistentHomology().detectSegments(mergedSample.getStorage().getStatistics(), mergedTrace).stream().filter(x->mergedTrace.intensity(x.apex) > stats.noiseLevel(x.apex)  ).toArray(TraceSegment[]::new);
        if (traceSegments.length==0 && alignedFeature.getUid()>=0) return null;
        // segments for each individual trace
//        TraceSegment[][] individualSegments = new TraceSegment[alignedFeature.getSampleIds().size()][];
//        for (int k=0; k < individualSegments.length; ++k) {
//            ProcessedSample sample = samplesInTrace[k];
//            assert sample.getUid() == alignedFeature.getSampleIds().getInt(k);
//            individualSegments[k] = assignSegmentsToIndividualTrace(mergedSample, sample, traceSegments, mergedTrace, mergedSample.getStorage().getMergeStorage().getTrace(alignedFeature.getTraceIds().getInt(k)) );
//        }
        final Int2ObjectOpenHashMap<ProcessedSample> uid2sample = new Int2ObjectOpenHashMap<>();
        for (ProcessedSample sample : samplesInTrace) {
            uid2sample.put(sample.getUid(), sample);
        }
        {
            ms2MergeStrategy.assignMs2(mergedSample, alignedFeature, traceSegments, uid2sample, new Ms2MergeStrategy.AssignMs2ToFeature() {
                @Override
                public void assignMs2ToFeature(MergedTrace mergedTrace, TraceSegment segment, MergedSpectrum mergedSpectrum) {
                    // TODO: do something with MS/MS
                }
            });
        }
        {
            StringBuffer buf = new StringBuffer();
            buf.append("{ \"rt\": [");
            List<String> xs = new ArrayList<>();
            for (int k=mergedTrace.startId(); k <= mergedTrace.endId(); ++k) {
                xs.add(String.valueOf(mergedTrace.retentionTime(k)));
            }
            buf.append(String.join( ", ", xs));
            buf.append("], \"parent\": [");
            xs.clear();
            for (int k=mergedTrace.startId(); k <= mergedTrace.endId(); ++k) {
                xs.add(String.valueOf(mergedTrace.intensity(k)));
            }
            buf.append(String.join( ", ", xs));
            buf.append("], \"children\": {");
            for (int s=0; s < samplesInTrace.length; ++s) {
                buf.append("\"" + samplesInTrace[s].getUid() + "\": [");
                xs.clear();
                final ProcessedSample S = samplesInTrace[s];
                final Trace t = mergedSample.getStorage().getMergeStorage().getTrace(alignedFeature.getTraceIds().getInt(s));
                for (int k=mergedTrace.startId(); k <= mergedTrace.endId(); ++k) {
                    xs.add(String.valueOf(S.getScanPointInterpolator().interpolateIntensity(t, k)));
                }
                buf.append(String.join( ", ", xs));
                buf.append("]");
                if (s+1 < samplesInTrace.length) buf.append(", ");
            }
            buf.append("}, ");
            buf.append("\"isotopes\": [" + alignedFeature.getIsotopeUids().intStream().mapToObj(Integer::toString).collect(Collectors.joining(", ")) + "],");
            buf.append("\"normalization\": {");
            for (int s=0; s < samplesInTrace.length; ++s) {
                ProcessedSample S = samplesInTrace[s];
                buf.append("\"" + samplesInTrace[s].getUid() + "\": " + S.getNormalizer().normalize(1));
                if (s+1 < samplesInTrace.length) buf.append(", ");
            }
            buf.append("}, ");
            buf.append("\"noise\": [");
            for (int k=mergedTrace.startId(); k <= mergedTrace.endId(); ++k) {
                double noise = stats.noiseLevel(k);
                buf.append(noise);
                if (k < mergedTrace.endId()) buf.append(", ");
            }
            buf.append("], ");
            buf.append("\"mz\": " + mergedTrace.averagedMz() + ", \"apexRt\": " + mergedTrace.retentionTime(mergedTrace.apex()) + ", ");
            buf.append("\"segments\": [");
            xs.clear();
            final int o = mergedTrace.startId();
            for (int k=0; k < traceSegments.length; ++k) {
                xs.add("{\"left\":" + (traceSegments[k].leftEdge-o) + ", \"right\": " + (traceSegments[k].rightEdge-o) + ", \"apex\": " + (traceSegments[k].apex-o) + "}");
            }
            buf.append(String.join( ", ", xs));
            buf.append("]}");
            return buf.toString();
        }
    }

    private TraceSegment[] assignSegmentsToIndividualTrace(ProcessedSample sample, TraceSegment[] mergedSegments, ContiguousTrace trace) {
        // easiest one: just map parent to childs
        final TraceSegment[] childSegments = new TraceSegment[mergedSegments.length];
        for (int k=0; k < mergedSegments.length; ++k) {
            TraceSegment s = mergedSegments[k];
            if (s == null)
                continue;

            int a = Math.max(trace.startId(), sample.getScanPointInterpolator().reverseMapLowerIndex(s.leftEdge));
            int b = Math.min(trace.endId(), sample.getScanPointInterpolator().reverseMapLargerIndex(s.rightEdge));
            if (b <= a) {
                continue;
            }

            childSegments[k] = new TraceSegment(a, a, b);
        }

        return resolveSegments(childSegments, trace);
    }

    private TraceSegment[] assignIntervalstoTrace(ProcessedSample sample, MergedTrace trace, double[][] intervals) {
        final TraceSegment[] childSegments = new TraceSegment[intervals.length];

        for (int k=0; k < intervals.length; k++) {
            final int a = findNearestIndex(sample, trace, intervals[k][0]);
            final int b = findNearestIndex(sample, trace, intervals[k][1]);
            if (b <= a)
                continue;

            childSegments[k] = new TraceSegment(a, a, b);
        }

        return resolveSegments(childSegments, trace.toTrace(sample));
    }

    private TraceSegment[] resolveSegments(TraceSegment[] segments, Trace trace) {
        // resolve overlapping segments
        for (int k=0; k < segments.length - 1; ++k) {
            if (segments[k] == null || segments[k + 1] == null) continue;
            if (segments[k + 1].leftEdge < segments[k].rightEdge) {
                int valley = segments[k + 1].leftEdge;
                for (int j=segments[k + 1].leftEdge; j <= segments[k].rightEdge; ++j){
                    if (trace.intensity(j) < trace.intensity(valley)) {
                        valley=j;
                    }
                }
                segments[k].rightEdge = valley;
                segments[k + 1].leftEdge = valley;
            }
        }

        // filter empty segments and find apex
        for (int k=0; k < segments.length; ++k) {
            if (segments[k] == null) continue;
            if (segments[k].leftEdge == segments[k].rightEdge) {
                segments[k] = null;
                continue;
            }
            segments[k].apex = segments[k].leftEdge;
            for (int j=segments[k].leftEdge; j <= segments[k].rightEdge; ++j){
                if (trace.intensity(j) > trace.intensity(segments[k].apex)) {
                    segments[k].apex=j;
                }
            }
        }

        return segments;
    }

    private int findNearestIndex(ProcessedSample sample, MergedTrace mergedTrace, double rt) {
        Trace trace = mergedTrace.toTrace(sample);
        double diff = Double.POSITIVE_INFINITY;
        int res = mergedTrace.getStartId();
        for (int i = mergedTrace.getStartId(); i <= mergedTrace.getEndId(); i++) {
            if (Math.abs(rt - trace.retentionTime(i)) < diff) {
                diff = Math.abs(rt - trace.retentionTime(i));
                res = i;
            }
        }
        return res;
    }


}
