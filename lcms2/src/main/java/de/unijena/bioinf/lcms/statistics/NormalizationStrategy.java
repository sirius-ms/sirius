package de.unijena.bioinf.lcms.statistics;

import de.unijena.bioinf.lcms.trace.ProcessedSample;

public interface NormalizationStrategy {

    public interface Normalizer {
        public double normalize(double intensity);
    }

    public Normalizer computeNormalization(ProcessedSample sample);

}
