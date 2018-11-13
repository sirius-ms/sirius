package de.unijena.bioinf.ChemistryBase.ms;

public interface DatasetStatistics {


    public double getMinMs1Intensity();
    public double getMaxMs1Intensity();

    public double getMinMs2Intensity();
    public double getMaxMs2Intensity();

    public double getMinMs2NoiseIntensity();
    public double getMaxMs2NoiseIntensity();
    public double getMeanMs2NoiseIntensity();
    public double getMedianMs2NoiseIntensity();
}
