package de.unijena.bioinf.lcms.features;

import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.msms.Ms2MergeStrategy;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.ProjectedTrace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;

import java.util.Iterator;

public interface MergedFeatureExtractionStrategy {

    public TraceSegment[] extractMergedSegments(ProcessedSample mergedSample, MergedTrace mergedTrace);

    public TraceSegment[][] extractProjectedSegments(ProcessedSample mergedSample, MergedTrace mergedTrace, TraceSegment[] mergedTraceSegments);

}
