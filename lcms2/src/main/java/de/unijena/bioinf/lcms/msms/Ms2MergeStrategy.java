package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.ArrayList;

public interface Ms2MergeStrategy {

    public MergedSpectrum[][] assignMs2(ProcessedSample mergedSample, MergedTrace trace, TraceSegment[] mergedSegments, TraceSegment[][] projectedSegments);
}
