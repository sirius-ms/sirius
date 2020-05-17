package de.unijena.bioinf.lcms.noise;

import de.unijena.bioinf.ChemistryBase.algorithm.Quickselect;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.model.lcms.Scan;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;

public class NoiseStatistics {

    private float[] noise;
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

    public NoiseModel getLocalNoiseModel() {
        done();
        return new LocalNoiseModel(noiseLevels.toArray(), scanNumbers.toArray());
    }

    private void done() {
        if (len < noise.length) {
            double v = avgNoise/len;
            for (int k = 0; k < len; ++k) noiseLevels.add((float)v);
            noise = Arrays.copyOf(noise,len);
        }
    }

    public NoiseModel getGlobalNoiseModel() {
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
        float lowestRecordedIntensity = Float.POSITIVE_INFINITY;
        int numberOnNonZeros = 0;
        for (double i : array) {
            if ((float)i>0) {
                lowestRecordedIntensity = Math.min(lowestRecordedIntensity, (float)i);
                ++numberOnNonZeros;
            }
        }
        if (numberOnNonZeros>=100) {
            int k = (int)Math.floor(array.length*percentile);
            float fl = (float)Quickselect.quickselectInplace(array,0,array.length, k);
            if (fl<=0) return lowestRecordedIntensity/5f;
            else return fl;
        } else {
            return lowestRecordedIntensity/5f;
        }

    }
}
