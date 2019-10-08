package de.unijena.bioinf.lcms.quality;

/**
 * Denotes that the instance does not align well with other instances
 */
public class AlignmentQuality implements QualityAnnotation {

    private static final AlignmentQuality NO_ALIGNMENT = new AlignmentQuality(0,0);

    public static AlignmentQuality none() {
        return NO_ALIGNMENT;
    }

    protected final int numberOfAlignedFeatures, medianNumberOfAlignedFeatures;

    public AlignmentQuality(int numberOfAlignedFeatures, int medianNumberOfAlignedFeatures) {
        this.numberOfAlignedFeatures = numberOfAlignedFeatures;
        this.medianNumberOfAlignedFeatures = medianNumberOfAlignedFeatures;
    }

    @Override
    public Quality getQuality() {
        if (numberOfAlignedFeatures < 10) {
            return Quality.UNUSABLE;
        }
        double pr = ((double)numberOfAlignedFeatures)/ medianNumberOfAlignedFeatures;
        if (pr < 0.1) return Quality.BAD;
        if (pr < 0.25) return Quality.DECENT;
        return Quality.GOOD;
    }

    @Override
    public String toString() {
        return "Alignment of " + numberOfAlignedFeatures + ", median is " + medianNumberOfAlignedFeatures;
    }
}
