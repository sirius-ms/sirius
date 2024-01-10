package de.unijena.bioinf.lcms.features;

import de.unijena.bioinf.lcms.merge2.MergedTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.AlignedFeatures;

import java.util.Iterator;

public interface MergedFeatureExtractionStrategy {
    public Iterator<AlignedFeatures> extractFeatures(ProcessedSample mergedSample, ProcessedSample[] samplesInTrace, MergedTrace mergedTrace);
}
