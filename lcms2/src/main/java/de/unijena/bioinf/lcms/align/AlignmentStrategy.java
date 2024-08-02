package de.unijena.bioinf.lcms.align;

import de.unijena.bioinf.lcms.trace.ProcessedSample;

import java.util.List;

public interface AlignmentStrategy {

    public AlignmentBackbone makeAlignmentBackbone(AlignmentStorage storage, List<ProcessedSample> samples, AlignmentAlgorithm algorithm, AlignmentScorer scorer);

    /**
     * Align all MoIs from all samples into merge. The method has no return value. Instead, all aligned MoIs are stored
     * into the merge storage
     */
    public AlignmentBackbone align(ProcessedSample merge, AlignmentBackbone backbone, List<ProcessedSample> samples, AlignmentAlgorithm algorithm, AlignmentScorer scorer);

}
