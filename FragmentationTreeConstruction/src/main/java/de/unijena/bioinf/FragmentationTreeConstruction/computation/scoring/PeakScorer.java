package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.FragmentationTreeConstruction.inspection.Inspectable;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

public interface PeakScorer {

    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores);

}
