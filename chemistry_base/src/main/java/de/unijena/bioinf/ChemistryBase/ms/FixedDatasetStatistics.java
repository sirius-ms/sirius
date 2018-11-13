package de.unijena.bioinf.ChemistryBase.ms;

public class FixedDatasetStatistics implements DatasetStatistics {

    private final double minMs1Intensity;
    private final double maxMs1Intensity;
    private final double minMs2Intensity;
    private final double maxMs2Intensity;

    private final double minMs2NoiseIntensity;
    private final double maxMs2NoiseIntensity;
    private final double meanMs2NoiseIntensity;
    private final double medianMs2NoiseIntensity;

    public FixedDatasetStatistics(double minMs1Intensity, double maxMs1Intensity, double minMs2Intensity, double maxMs2Intensity, double minMs2NoiseIntensity, double maxMs2NoiseIntensity, double meanMs2NoiseIntensity, double medianMs2NoiseIntensity) {
        this.minMs1Intensity = minMs1Intensity;
        this.maxMs1Intensity = maxMs1Intensity;
        this.minMs2Intensity = minMs2Intensity;
        this.maxMs2Intensity = maxMs2Intensity;
        this.minMs2NoiseIntensity = minMs2NoiseIntensity;
        this.maxMs2NoiseIntensity = maxMs2NoiseIntensity;
        this.meanMs2NoiseIntensity = meanMs2NoiseIntensity;
        this.medianMs2NoiseIntensity = medianMs2NoiseIntensity;
    }


    public FixedDatasetStatistics(DatasetStatistics datasetStatistics) {
        this.minMs1Intensity = datasetStatistics.getMinMs1Intensity();
        this.maxMs1Intensity = datasetStatistics.getMaxMs1Intensity();
        this.minMs2Intensity = datasetStatistics.getMinMs2Intensity();
        this.maxMs2Intensity = datasetStatistics.getMaxMs2Intensity();
        this.minMs2NoiseIntensity = datasetStatistics.getMinMs2NoiseIntensity();
        this.maxMs2NoiseIntensity = datasetStatistics.getMaxMs2NoiseIntensity();
        this.meanMs2NoiseIntensity = datasetStatistics.getMeanMs2NoiseIntensity();
        this.medianMs2NoiseIntensity = datasetStatistics.getMedianMs2NoiseIntensity();
    }


    @Override
    public double getMinMs1Intensity() {
        return minMs1Intensity;
    }

    @Override
    public double getMaxMs1Intensity() {
        return maxMs1Intensity;
    }

    @Override
    public double getMinMs2Intensity() {
        return minMs2Intensity;
    }

    @Override
    public double getMaxMs2Intensity() {
        return maxMs2Intensity;
    }

    @Override
    public double getMinMs2NoiseIntensity() {
        return minMs2NoiseIntensity;
    }

    @Override
    public double getMaxMs2NoiseIntensity() {
        return maxMs2NoiseIntensity;
    }

    @Override
    public double getMeanMs2NoiseIntensity() {
        return meanMs2NoiseIntensity;
    }

    @Override
    public double getMedianMs2NoiseIntensity() {
        return medianMs2NoiseIntensity;
    }

}
