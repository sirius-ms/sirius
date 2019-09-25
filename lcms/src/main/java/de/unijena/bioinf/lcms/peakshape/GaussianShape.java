package de.unijena.bioinf.lcms.peakshape;

import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.lcms.quality.Quality;

public class GaussianShape implements PeakShape {

    protected double score;
    protected double mean;
    protected double standardDeviation, maxIntensity;

    public GaussianShape(double score, double mean, double standardDeviation, double maxIntensity) {
        this.score = score;
        this.mean = mean;
        this.standardDeviation = standardDeviation;
        this.maxIntensity = maxIntensity;
    }

    public double getLocation() {
        return mean;
    }

    @Override
    public Quality getPeakShapeQuality() {
        if (score < -0.6) return Quality.UNUSABLE;
        if (score < -0.4) return Quality.BAD;
        if (score < -0.25) return Quality.DECENT;
        return Quality.GOOD;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public double expectedIntensityAt(long k) {
        final NormalDistribution distribution = new NormalDistribution(mean, standardDeviation * standardDeviation);
        return maxIntensity*distribution.getDensity(k)/distribution.getDensity(mean);
    }

    @Override
    public double getScore() {
        return score;
    }
}
