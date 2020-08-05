
package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

import java.util.List;

public interface PeakScorer extends Parameterized {

    void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores);

}
