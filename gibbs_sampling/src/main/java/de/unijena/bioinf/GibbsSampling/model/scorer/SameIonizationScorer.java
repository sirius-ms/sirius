package de.unijena.bioinf.GibbsSampling.model.scorer;

import de.unijena.bioinf.GibbsSampling.model.EdgeScorer;
import de.unijena.bioinf.GibbsSampling.model.FragmentsCandidate;

import java.util.Arrays;

public class SameIonizationScorer implements EdgeScorer<FragmentsCandidate> {
    private double differentIonizationLogProbability;
    private static final double  DEFAULT_DIFF_IONIZATION_LOG_PROBABILITY=Math.log(0.01);

    public SameIonizationScorer() {
        this(DEFAULT_DIFF_IONIZATION_LOG_PROBABILITY);
    }

    public SameIonizationScorer(double differentIonizationLogProbability) {
        this.differentIonizationLogProbability = differentIonizationLogProbability;
    }

    @Override
    public void setThreshold(double threshold) {
        //todo no threshold
    }

    @Override
    public double getThreshold() {
        return 0;
    }

    @Override
    public void prepare(FragmentsCandidate[][] var1) {

    }

    @Override
    public double score(FragmentsCandidate var1, FragmentsCandidate var2) {
        if (var1.getIonType().getIonization().equals(var2.getIonType().getIonization())){
            return 0;
        } else {
            //Todo the score was the other way around?
            return -differentIonizationLogProbability;
        }
    }

    @Override
    public double scoreWithoutThreshold(FragmentsCandidate var1, FragmentsCandidate var2) {
        return score(var1, var2);
    }

    @Override
    public void clean() {

    }

    @Override
    public double[] normalization(FragmentsCandidate[][] var1, double minimum_number_matched_peaks_losses) {
        //todo not used?
        double[] norm = new double[var1.length];
        Arrays.fill(norm, 1d);
        return norm;
    }
}
