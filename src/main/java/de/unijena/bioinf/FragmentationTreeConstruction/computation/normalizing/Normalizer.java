package de.unijena.bioinf.FragmentationTreeConstruction.computation.normalizing;

import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSExperimentInformation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSInput;

import java.util.List;

/**
 * @author Kai Dührkop
 */
public interface Normalizer {
    /**
     * returns normalized intensity values for all peaks
     * @param input
     * @param information
     * @param peaks
     * @return normalized intensity array, where array[i] is corresponding to the intensity of peaks[i]
     */
    public double[] normalize(MSInput input, MSExperimentInformation information, List<MS2Peak> peaks);

}
