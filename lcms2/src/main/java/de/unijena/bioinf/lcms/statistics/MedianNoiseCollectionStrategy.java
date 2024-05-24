package de.unijena.bioinf.lcms.statistics;

import de.unijena.bioinf.ChemistryBase.algorithm.Quickselect;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.spectrum.Ms1SpectrumHeader;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 *
 */
public class MedianNoiseCollectionStrategy implements StatisticsCollectionStrategy {

    @Override
    public Calculation collectStatistics() {
        return new CalculateMedians();
    }

    protected static class CalculateMedians implements Calculation {
        private FloatArrayList noise = new FloatArrayList();
        private FloatArrayList ms2Noise = new FloatArrayList();

        @Override
        public void processMs1(Ms1SpectrumHeader header, SimpleSpectrum ms1Spectrum) {
            double[] xs = Spectrums.copyIntensities(ms1Spectrum);
            if (xs.length==0) {
                LoggerFactory.getLogger(MedianNoiseCollectionStrategy.class).warn("Empty MS1 spectrum found.");
                noise.add((float)noise.doubleStream().average().orElse(0d));
                return;
            }
            int perc = (int)(0.9*xs.length);
            double noiseLevel = Quickselect.quickselectInplace(xs, 0, xs.length, perc);
            noise.add((float)noiseLevel);
        }

        @Override
        public void processMs2(Ms2SpectrumHeader header, SimpleSpectrum ms2Spectrum) {
            double[] xs = Spectrums.copyIntensities(ms2Spectrum);
            if (xs.length==0) {
                LoggerFactory.getLogger(MedianNoiseCollectionStrategy.class).warn("Empty MS2 spectrum found.");
                noise.add((float)noise.doubleStream().average().orElse(0d));
                return;
            }
            if (xs.length <= 10) {
                // ignore spectra that are almost empty
                return;
            }
            int perc = Math.min(xs.length-10, (int)(0.75*xs.length));
            double noiseLevel = Quickselect.quickselectInplace(xs, 0, xs.length, perc);
            double minimumIntensity = Arrays.stream(xs).min().orElse(noiseLevel);
            // I have the feeling the noise level is a bit too high, so I incorporate the minimum noise to it.
            noiseLevel = (2*noiseLevel+minimumIntensity)/3d;
            ms2Noise.add((float)noiseLevel);
        }

        @Override
        public SampleStats done() {
            final float ms2NoiseAvg = (float)Statistics.robustAverage(ms2Noise.toFloatArray());
            final float[] ms1Noises = noise.toFloatArray();
            if (ms1Noises.length <= 20) {
                Arrays.sort(ms1Noises);
                final float median = ms1Noises[ms1Noises.length/2];
                Arrays.fill(ms1Noises, median);
            } else {
                final int width = Math.min(200, ms1Noises.length/10);
                final float[] medians = new float[ms1Noises.length-width];
                for (int k=0; k < ms1Noises.length-width; ++k) {
                    float[] buf = new float[width];
                    System.arraycopy(ms1Noises, k, buf, 0, width);
                    Arrays.sort(buf);
                    medians[k] = buf[buf.length/2];
                }
                int j=0;
                for (; j < width; ++j) {
                    ms1Noises[j] = medians[0];
                }
                for (; j < ms1Noises.length; ++j) {
                    ms1Noises[j] = medians[j-width];
                }
            }


            return SampleStats.builder().noiseLevelPerScan(ms1Noises).ms2NoiseLevel(ms2NoiseAvg).ms1MassDeviationWithinTraces(new Deviation(6,3e-4)).minimumMs1MassDeviationBetweenTraces(new Deviation(6,3e-4)).build();
        }
    }

}
