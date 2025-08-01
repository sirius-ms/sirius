package de.unijena.bioinf.lcms.traceextractor;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

import java.util.BitSet;

public class PickIntensivePeaksInConsecutiveSpectraStrategy  implements TraceDetectionStrategy {


    @Override
    public void findPeaksForExtraction(ProcessedSample sample, TraceDetectionStrategy.Extract callback) {
        BitSet acceptedPeaksLeft = new BitSet();
        BitSet acceptedPeaksRight = new BitSet();
        final SampleStats stats = sample.getStorage().getStatistics();
        final Deviation D = stats.getMs1MassDeviationWithinTraces();
        for (int idx = 1, n = sample.getMapping().length(); idx < n;  ++idx) {
            // swap vectors
            {
                BitSet z = acceptedPeaksLeft;
                acceptedPeaksLeft = acceptedPeaksRight;
                acceptedPeaksRight = z;
                acceptedPeaksRight.clear();
            }
            SimpleSpectrum LEFT = sample.getStorage().getSpectrumStorage().getSpectrum(idx-1);
            SimpleSpectrum RIGHT = sample.getStorage().getSpectrumStorage().getSpectrum(idx);
            float noiseThresholdSingle = stats.noiseLevel(idx);
            final double noiseThresholdDoubled = (n +stats.noiseLevel(idx-1));
            acceptedPeaksRight.clear();

            int indexLeft = 0;
            int indexRight = 0;

            while (indexLeft < LEFT.size() && indexRight < RIGHT.size()) {
                final double lm = LEFT.getMzAt(indexLeft);
                final double rm = RIGHT.getMzAt(indexRight);
                if (D.inErrorWindow(lm, rm)) {
                    // we matched a peak
                    double pl = LEFT.getIntensityAt(indexLeft);
                    double pr = RIGHT.getIntensityAt(indexRight);
                    if (pl + pr > noiseThresholdDoubled) {

                        // have we seen this peak before?
                        if (!acceptedPeaksLeft.get(indexLeft)) {
                            if (pl > pr) {
                                callback.extract(sample, idx - 1, indexLeft, LEFT);
                            } else {
                                callback.extract(sample, idx, indexRight, RIGHT);
                            }
                        }
                        acceptedPeaksRight.set(indexRight);
                        ++indexLeft;
                        ++indexRight;
                        continue;
                    }
                }
                if (lm <= rm) {
                    ++indexLeft;
                } else {
                    ++indexRight;
                }

            }

        }
    }
}
