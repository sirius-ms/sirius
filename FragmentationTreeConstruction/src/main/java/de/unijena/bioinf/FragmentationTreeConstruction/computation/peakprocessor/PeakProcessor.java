package de.unijena.bioinf.FragmentationTreeConstruction.computation.peakprocessor;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.decomposing.Decomposer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSExperimentInformation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

public interface PeakProcessor {

    public static enum Stage {
        BEFORE_MERGING, AFTER_MERGING; // ...
    }

    public Stage getStage();

    public <T> void process(List<ProcessedPeak> peaks, MSInput input, MSExperimentInformation info, Decomposer<T> decomposer, T init);

}
