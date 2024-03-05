package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.ArrayList;

public interface Ms2MergeStrategy {

    interface AssignMs2ToFeature {
        void assignMs2ToFeature(MergedTrace mergedTrace, TraceSegment segment, int index, MergedSpectrum mergedSpectrum, ArrayList<MsMsQuerySpectrum> spectra);
    }

    void assignMs2(ProcessedSample mergedSample, MergedTrace trace, TraceSegment[] featureSegments, Int2ObjectOpenHashMap<ProcessedSample> otherSamples, AssignMs2ToFeature assigner);

}
