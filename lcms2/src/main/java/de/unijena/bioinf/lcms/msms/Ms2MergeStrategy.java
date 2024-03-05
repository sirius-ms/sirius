package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public interface Ms2MergeStrategy {

    interface AssignMs2ToFeature {
        void assignMs2ToFeature(MergedTrace mergedTrace, TraceSegment segment, int index, MergedSpectrum mergedSpectrum);
    }

    void assignMs2(ProcessedSample mergedSample, MergedTrace trace, TraceSegment[] featureSegments, Int2ObjectOpenHashMap<ProcessedSample> otherSamples, AssignMs2ToFeature assigner);

}
