package de.unijena.bioinf.lcms.peakshape;

import de.unijena.bioinf.lcms.quality.Quality;

public class CustomPeakShape implements PeakShape{

    protected Quality quality;
    protected double score;

    public CustomPeakShape(double probability) {
        this.score = Math.log(probability);
        if (probability >= 0.25) quality = Quality.GOOD;
        else if (probability >= 0.01) quality = Quality.DECENT;
        else if (probability > 0.001) quality = Quality.BAD;
        else quality = Quality.UNUSABLE;
    }

    @Override
    public double getScore() {
        return score;
    }

    @Override
    public double expectedIntensityAt(long rt) {
        return 0;
    }

    @Override
    public double getLocation() {
        return 0;
    }

    @Override
    public Quality getPeakShapeQuality() {
        return quality;
    }
}
