package de.unijena.bioinf.FragmentationTreeConstruction.computation.normalizing;

import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSExperimentInformation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSInput;

import java.util.List;

/**
 * @author Kai Dührkop
 */
public class IntensitySumNormalizer implements Normalizer {

    private final double maxVal;

    public IntensitySumNormalizer(double maxVal) {
        this.maxVal = maxVal;
    }

    public IntensitySumNormalizer() {
        this(1d);
    }

    @Override
    public double[] normalize(MSInput input, MSExperimentInformation information, List<MS2Peak> peaks) {
        final double[] normalizedIntensities = new double[peaks.size()];
        int k=0;
        for (MS2Peak p : peaks) normalizedIntensities[k++] = p.getIntensity();
        double sum = 0d;
        for (double val : normalizedIntensities) sum += val;
        final double scale = maxVal / sum;
        for (k = 0; k < normalizedIntensities.length; ++k) normalizedIntensities[k] *= scale;
        return normalizedIntensities;
    }
}
