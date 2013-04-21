package de.unijena.bioinf.FragmentationTreeConstruction.computation.merging;

import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

/*
    A merger which gets a list of peaks and merge them to a single peak. The implementation is given by
    FragmentationTreeAnalysis itself.
 */
public interface Merger {

    /**
     * Merge the given peaks to a single peak
     * @param peaks the peaks to merge. May be modified during the method call
     * @param index the index of the main peak which intensity is passed to the merged peak
     * @return the merged peak
     */
    public ProcessedPeak merge(List<ProcessedPeak> peaks, int index, double newMz);

}
