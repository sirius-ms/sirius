package de.unijena.bioinf.FragmentationTreeConstruction.computation.normalizing;

import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSExperimentInformation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSInput;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.summary.Sum;
import org.apache.commons.math3.util.MathArrays;

import java.util.Arrays;
import java.util.List;

/**
 * @author Kai Dührkop
 */
public class IntensityMaxNormalizer implements Normalizer {

    private final double maxVal;

    public IntensityMaxNormalizer(double maxVal) {
        this.maxVal = maxVal;
    }

    @Override
    public double[] normalize(MSInput input, MSExperimentInformation information, List<MS2Peak> peaks) {
        final double[] normalizedIntensities = new double[peaks.size()];
        if (normalizedIntensities.length==0) return normalizedIntensities;
        int k=0;
        for (MS2Peak p : peaks) normalizedIntensities[k++] = p.getIntensity();
        double max = normalizedIntensities[0];
        for (double n : normalizedIntensities) if (n > max) max = n;
        final double scale = maxVal / max;
        for (k=0; k < normalizedIntensities.length; ++k) normalizedIntensities[k] *= scale;
        return normalizedIntensities;
    }
}
