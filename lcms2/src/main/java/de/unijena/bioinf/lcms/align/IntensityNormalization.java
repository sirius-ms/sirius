package de.unijena.bioinf.lcms.align;

import de.unijena.bioinf.lcms.trace.Trace;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatIterable;

import java.util.Arrays;

/**
 * Intensities are just normalized for comparison between samples
 */
public abstract class IntensityNormalization {

    public interface Normalizer {
        public float normalize(float intensity);
    }

    public abstract Normalizer getNormalizer(FloatIterable apexIntensities);

    public final static class QuantileNormalizer extends IntensityNormalization {

        @Override
        public Normalizer getNormalizer(FloatIterable apexIntensities) {
            final float[] ints;
            {
                FloatArrayList intensities = new FloatArrayList();
                apexIntensities.forEach(intensities::add);
                intensities.unstableSort(null);
                ints = intensities.toFloatArray();
            }
            return new Normalizer() {
                @Override
                public float normalize(float intensity) {
                    int i = Arrays.binarySearch(ints, intensity);
                    if (i < 0) {
                        i=-i+1;
                    }
                    return ((float)i)/ints.length;
                }
            };

        }
    }

}
