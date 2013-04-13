package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSExperimentInformation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSInput;

import java.util.List;

public interface NoisePeakFilter {

    /**
     * Select all peaks in the given list which are definitely noise.
     * Calls {@link NoisePeakCallback#reportNoise(de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak, String)}
     * for each peak which is considered as noise.
     * @param peaks
     * @return a list of noise peaks which should be removed
     */
    public List<MS2Peak> filter(MSInput input, MSExperimentInformation informations, List<MS2Peak> peaks, NoisePeakCallback callback);

}
