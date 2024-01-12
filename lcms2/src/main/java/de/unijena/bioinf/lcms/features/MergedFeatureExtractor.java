package de.unijena.bioinf.lcms.features;

import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.lcms.trace.segmentation.PersistentHomology;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.ms.persistence.model.core.AlignedFeatures;

import java.util.*;

public class MergedFeatureExtractor implements MergedFeatureExtractionStrategy{

    @Override
    public Iterator<AlignedFeatures> extractFeatures(ProcessedSample mergedSample, ProcessedSample[] samplesInTrace, MergedTrace alignedFeature) {
        // segments for merged trace
        Trace mergedTrace = alignedFeature.toTrace(mergedSample);
        final SampleStats stats = mergedSample.getStorage().getStatistics();
        TraceSegment[] traceSegments = new PersistentHomology().detectSegments(mergedSample.getStorage().getStatistics(), mergedTrace).stream().filter(x->mergedTrace.intensity(x.apex) > stats.noiseLevel(x.apex)  ).toArray(TraceSegment[]::new);
        if (traceSegments.length==0) return Collections.emptyIterator();
        // segments for each individual trace
        TraceSegment[][] individualSegments = new TraceSegment[alignedFeature.getSampleIds().size()][];
        for (int k=0; k < individualSegments.length; ++k) {
            ProcessedSample sample = samplesInTrace[k];
            assert sample.getUid() == alignedFeature.getSampleIds().getInt(k);
            individualSegments[k] = assignSegmentsToIndividualTrace(mergedSample, sample, traceSegments, mergedTrace, mergedSample.getStorage().getMergeStorage().getTrace(alignedFeature.getTraceIds().getInt(k)) );
        }
        {

        }
        return null;
    }

    public String extractFeaturesToString(ProcessedSample mergedSample, ProcessedSample[] samplesInTrace, MergedTrace alignedFeature) {
        // segments for merged trace
        Trace mergedTrace = alignedFeature.toTrace(mergedSample);
        SampleStats stats = mergedSample.getStorage().getStatistics();
        TraceSegment[] traceSegments = new PersistentHomology().detectSegments(mergedSample.getStorage().getStatistics(), mergedTrace).stream().filter(x->mergedTrace.intensity(x.apex) > stats.noiseLevel(x.apex)  ).toArray(TraceSegment[]::new);
        if (traceSegments.length==0) return null;
        // segments for each individual trace
        TraceSegment[][] individualSegments = new TraceSegment[alignedFeature.getSampleIds().size()][];
        for (int k=0; k < individualSegments.length; ++k) {
            ProcessedSample sample = samplesInTrace[k];
            assert sample.getUid() == alignedFeature.getSampleIds().getInt(k);
            individualSegments[k] = assignSegmentsToIndividualTrace(mergedSample, sample, traceSegments, mergedTrace, mergedSample.getStorage().getMergeStorage().getTrace(alignedFeature.getTraceIds().getInt(k)) );
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

    protected TraceSegment[] assignSegmentsToIndividualTrace(ProcessedSample mergedSample, ProcessedSample sample, TraceSegment[] mergedSegments, Trace mergedTrace, ContiguousTrace trace) {
        // easiest one: just map parent to childs
        final TraceSegment[] childSegments = new TraceSegment[mergedSegments.length];
        for (int k=0; k < mergedSegments.length; ++k) {
            TraceSegment s = mergedSegments[k];
            int a = Math.max(trace.startId(), sample.getScanPointInterpolator().reverseMapLowerIndex(s.leftEdge));
            int b = Math.min(trace.endId(), sample.getScanPointInterpolator().reverseMapLargerIndex(s.rightEdge));
            if (b<a) {
                childSegments[k]=null;
                continue;
            }
            int apex = a;
            for (int j=a; j <= b; ++j){
                if (trace.intensity(j)>trace.intensity(apex)) {
                    apex=j;
                }
            }
            childSegments[k] = new TraceSegment(apex, a, b);
        }
        // resolve overlapping segments
        int edge = -1;
        for (int k=0; k < childSegments.length; ++k) {
            if (childSegments[k]!=null) {
                if (childSegments[k].leftEdge < edge) {
                    childSegments[k].leftEdge=edge;
                }
                edge = childSegments[k].rightEdge;
            }
        }

        return childSegments;
    }


}
