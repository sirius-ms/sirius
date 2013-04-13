package de.unijena.bioinf.FragmentationTreeConstruction.computation.merging;

import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

public interface Merger {

    public void merge(List<ProcessedPeak> merged, int mainIndex, double mz, double newRelativeGlobalIntensity);

}
