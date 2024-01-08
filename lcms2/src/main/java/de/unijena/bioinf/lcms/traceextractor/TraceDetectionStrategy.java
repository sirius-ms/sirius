package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

/**
 * Extracts all traces from a sample and stores them
 */
public interface TraceDetectionStrategy {

    public interface Extract {
        public void extract(ProcessedSample sample, int spectrumIdx, int peakIdx, SimpleSpectrum spectrum);
    }

    /**
     * returns (spectrum idx, peak idx) pairs that should be extracted
     */
    public void findPeaksForExtraction(ProcessedSample sample, Extract callback);

}
