package de.unijena.bioinf.ChemistryBase.ms;

import gnu.trove.list.array.TDoubleArrayList;

/**
 * Created by ge28quv on 02/07/17.
 */
public class DatasetStatistics {

    private TDoubleArrayList minMs1Intensities = new TDoubleArrayList();
    private TDoubleArrayList maxMs1Intensities = new TDoubleArrayList();
    private TDoubleArrayList minMs2Intensities = new TDoubleArrayList();
    private TDoubleArrayList maxMs2Intensities = new TDoubleArrayList();

    private TDoubleArrayList noiseIntensities = new TDoubleArrayList(); //ms2


    private double medianNoiseIntensity = Double.NaN;

    public DatasetStatistics(){

    }



    public void addMinMs1Intensity(double intensity){
        minMs1Intensities.add(intensity);
    }

    public void addMaxMs1Intensity(double intensity){
        maxMs1Intensities.add(intensity);
    }

    public void addMinMs2Intensity(double intensity){
        minMs2Intensities.add(intensity);
    }

    public void addMaxMs2Intensity(double intensity){
        maxMs2Intensities.add(intensity);
    }

    public void addMs2NoiseIntensity(double intensity){
        if (!Double.isNaN(medianNoiseIntensity)) medianNoiseIntensity = Double.NaN;
        noiseIntensities.add(intensity);
    }


    public double getMinMs1Intensity(){
        return minMs1Intensities.min();
    }

    public double getMaxMs1Intensity(){
        return maxMs1Intensities.max();
    }

    public double getMinMs2Intensity(){
        return minMs2Intensities.min();
    }

    public double getMaxMs2Intensity(){
        return maxMs2Intensities.max();
    }

    public double getMinMs2NoiseIntensity(){
        return noiseIntensities.min();
    }

    public double getMaxMs2NoiseIntensity(){
        return noiseIntensities.max();
    }

    public double getMeanMs2NoiseIntensity(){
        return noiseIntensities.sum()/noiseIntensities.size();
    }

    public double getMedianMs2NoiseIntensity(){
        //todo implement fast median
        if (Double.isNaN(medianNoiseIntensity)){
            if (noiseIntensities.size()==0) throw new IllegalStateException("Cannot estimate median noise intensity. No noise peaks found.");
            TDoubleArrayList copy = new TDoubleArrayList(noiseIntensities);
            copy.sort();
            medianNoiseIntensity = copy.get(copy.size()/2);
        }
        return medianNoiseIntensity;
    }

    public double getQuantileMs2NoiseIntensity(int quantile){
        //todo implement fast median
        if (noiseIntensities.size()==0) throw new RuntimeException("cannot estimate median noise intensity.");
        TDoubleArrayList copy = new TDoubleArrayList(noiseIntensities);
        copy.sort();
        return copy.get((int)(copy.size()*(quantile/100d)));
    }

    public TDoubleArrayList getNoiseIntensities() {
        return noiseIntensities;
    }
}
