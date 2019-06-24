package de.unijena.bioinf.lcms.peakshape;

import de.unijena.bioinf.lcms.quality.Quality;
import org.apache.commons.math3.distribution.LaplaceDistribution;

public class LaplaceShape implements PeakShape {

    protected LaplaceDistribution distribution;
    protected double score;
    protected double maxIntensity;

    public LaplaceShape(double score, long median, double deviation, double maxIntensity) {
        this.distribution = new LaplaceDistribution(median, deviation);
        this.score = score;
        this.maxIntensity = maxIntensity;
    }

    public LaplaceDistribution getDistribution() {
        return distribution;
    }

    public double getScore() {
        return score;
    }

    public double getMaxIntensity() {
        return maxIntensity;
    }

    public double expectedIntensityAt(long k) {
        return maxIntensity*distribution.density(k)/distribution.density(distribution.getLocation());
    }

    @Override
    public double getLocation() {
        return distribution.getLocation();
    }

    @Override
    public Quality getPeakShapeQuality() {
        if (score < -0.6) return Quality.UNUSABLE;
        if (score < -0.4) return Quality.BAD;
        if (score < -0.25) return Quality.DECENT;
        return Quality.GOOD;
    }

}
