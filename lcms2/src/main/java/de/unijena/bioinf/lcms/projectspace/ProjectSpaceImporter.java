package de.unijena.bioinf.lcms.projectspace;

import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;

import java.io.IOException;

public interface ProjectSpaceImporter<Indexer> {

    public Indexer initializeImport(SiriusDatabaseAdapter adapter);

    /**
     * Is called once for every run AFTER preprocessing. At this point, LCMS runs are already in the
     * project space, as they are imported during mzml parsing (not really nice, but probably not easy to do otherwise).
     * So this method can update the run document to include information available after preprocessing
     */
    public void importRun(SiriusDatabaseAdapter adapter, Indexer indexer, ProcessedSample sample) throws IOException;


    /**
     * Similar to importRun this method is called after preprocessing to import the merged run.
     */
    public void importMergedRun(SiriusDatabaseAdapter adapter, Indexer indexer, ProcessedSample sample) throws IOException;

    /**
     * Imports the merged trace and extract all its compounds and isotopes. This is the central method for importing
     * features into the project space.
     */
    public AlignedFeatures[] importMergedTrace(TraceSegmentationStrategy traceSegmenter, SiriusDatabaseAdapter adapter, Indexer indexer, ProcessedSample mergedSample, MergedTrace mergedTrace, boolean allowMs1Only) throws IOException;

}
