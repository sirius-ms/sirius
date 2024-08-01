package de.unijena.bioinf.lcms.statistics;

import de.unijena.bioinf.ChemistryBase.algorithm.BoundedDoubleQueue;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;

public class AverageOfTop100TracesNormalization implements NormalizationStrategy{


    @Override
    public Normalizer computeNormalization(ProcessedSample sample) {
        BoundedDoubleQueue queue = new BoundedDoubleQueue(100);
        for (ContiguousTrace m : sample.getStorage().getTraceStorage()) {
            queue.add(m.apexIntensity());
        }
        double avg = 0d;
        for (double q : queue) avg += q;
        avg /= queue.length();
        return new ConstantNormalizer(avg);
    }

    private static class ConstantNormalizer implements Normalizer {
        private final double normConst;

        public ConstantNormalizer(double normConst) {
            this.normConst = normConst;
        }

        @Override
        public double normalize(double intensity) {
            return intensity/normConst;
        }
    }
}
