package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.FragmentationTreeConstruction.model.MSExperimentInformation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

/**
 * @author Kai Dührkop
 */
public interface PostProcessedNoisePeakFilter {

    /**
     * Select all peaks in the given list which are definitely noise.
     * Calls {@link NoisePeakCallback#reportNoise(de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak, String)}
     * for each peak which is considered as noise.
     * @param input
     * @param informations
     * @param peaks
     * @param callback
     * @return
     */
    public List<ProcessedPeak> filter(ProcessedInput input, MSExperimentInformation informations, List<ProcessedPeak> peaks,
                             NoisePeakCallback callback);

}
