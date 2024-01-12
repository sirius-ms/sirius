package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

public class PickIntensivePeaksDetectionStrategy implements TraceDetectionStrategy {
    @Override
    public void findPeaksForExtraction(ProcessedSample sample, Extract callback) {
        final SampleStats stats = sample.getStorage().getStatistics();
        for (int idx = 0, n = sample.getMapping().length(); idx < n;  ++idx) {
            SimpleSpectrum spectrum = sample.getStorage().getSpectrumStorage().getSpectrum(idx);
            final double noiseThreshold = stats.noiseLevel(idx);
            for (int k=0; k < spectrum.size(); ++k) {
                if (spectrum.getIntensityAt(k) > noiseThreshold) {
                    callback.extract(sample, idx, k, spectrum);
                }
            }
        }
    }
}
