package de.unijena.bioinf.lcms.features;

import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.msms.Ms2MergeStrategy;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.AlignedFeatures;

import java.util.Iterator;

public interface MergedFeatureExtractionStrategy {
    public Iterator<AlignedFeatures> extractFeatures(ProcessedSample mergedSample, ProcessedSample[] samplesInTrace, MergedTrace mergedTrace, Ms2MergeStrategy ms2MergeStrategy);
}
