package de.unijena.bioinf.FragmentationTreeConstruction.computation.merging;

import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

/**
 * @author Kai Dührkop
 */
public interface PeakMerger {

    public void mergePeaks(List<ProcessedPeak> peaks, ProcessedInput input, Merger merger);

}
