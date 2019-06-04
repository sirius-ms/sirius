package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.Random;

/**
 * Sample datapoints from scans.
 * Assume that noise is same level everywhere
 */
public class GlobalNoiseModel implements NoiseModel {

    private final double noiseLevel, signalLevel;

    public GlobalNoiseModel(SpectrumStorage storage, Iterable<Scan> scans, double percentile) {
        final TDoubleArrayList intensities = new TDoubleArrayList();
        final TDoubleArrayList noiseLevels = new TDoubleArrayList();
        final Random r = new Random();
        for (Scan scan : scans) {
            final SimpleSpectrum ms = storage.getScan(scan);
            if (ms.size() <= 60) {
                // seems to be prefiltered.
                continue;
            }
            // sample from spectrum
            int n = Math.max(1, ms.size()/100);
            int k = (n>1) ? r.nextInt(n) : 0;
            for (; k < ms.size(); k += n) {
                intensities.add(ms.getIntensityAt(k));
            }
            intensities.sort();
            // take 75% quantile
            final double noise = intensities.get((int)(intensities.size()*percentile));
            noiseLevels.add(noise);
        }
        noiseLevels.sort();
        this.noiseLevel = noiseLevels.isEmpty() ? 0d : noiseLevels.get((int)(noiseLevels.size()*0.5d));
        this.signalLevel = noiseLevel*10;
    }

    @Override
    public double getNoiseLevel(int scanNumber, double mz) {
        return noiseLevel;
    }

    @Override
    public double getSignalLevel(int scanNumber, double mz) {
        return signalLevel;
    }

    @Override
    public String toString() {
        return "GlobalNoiseModel{" +
                "noiseLevel=" + noiseLevel +
                ", signalLevel=" + signalLevel +
                '}';
    }
}
