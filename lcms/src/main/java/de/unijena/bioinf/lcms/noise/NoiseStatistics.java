package de.unijena.bioinf.lcms.noise;

import de.unijena.bioinf.ChemistryBase.algorithm.Quickselect;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.model.lcms.Scan;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;

public class NoiseStatistics {

    private final float[] noise;
    private int offset, len;
    private TIntArrayList scanNumbers;
    private TFloatArrayList noiseLevels;
    private float avgNoise;
    private double percentile;

    public NoiseStatistics(int bandWidth, double percentile) {
        this.noise = new float[bandWidth];
        this.offset = 0;
        this.len = 0;
        this.scanNumbers = new TIntArrayList();
        this.noiseLevels = new TFloatArrayList();
        this.avgNoise = 0;
        this.percentile = percentile;
    }

    public LocalNoiseModel getLocalNoiseModel() {
        return new LocalNoiseModel(noiseLevels.toArray(), scanNumbers.toArray());
    }
    public GlobalNoiseModel getGlobalNoiseModel() {
        final double noiseLevel = Quickselect.quickselectInplace(noiseLevels.toArray(), 0, noiseLevels.size (),(int)Math.floor(noiseLevels.size()*0.5));
        return new GlobalNoiseModel(noiseLevel, noiseLevel*10);
    }

    public void add(Scan scan, SimpleSpectrum spectrum) {
        scanNumbers.add(scan.getIndex());
        final float nl =calculateNoiseLevel(spectrum);
        if (len < noise.length) {
            noise[len++] = nl;
            avgNoise += nl;
            if (len==noise.length) {
                double v = avgNoise/len;
                for (int k = 0; k < noise.length; ++k) noiseLevels.add((float)v);
            }
        } else {
            avgNoise -= noise[offset];
            avgNoise += nl;
            noise[offset++] = nl;
            if (offset >= noise.length) offset = 0;
            noiseLevels.add(avgNoise/len);
        }
    }
    // TODO: for MS/MS use decomposer
    private float calculateNoiseLevel(SimpleSpectrum spectrum) {
        final double[] array = Spectrums.copyIntensities(spectrum);
        if (array.length>=60) {
            int k = (int)Math.floor(array.length*percentile);
            return (float)Quickselect.quickselectInplace(array,0,array.length, k);
        } else {
            // there might be already a noise cutoff?
            return (float)Arrays.stream(array).min().orElse(0);
        }

    }
}
