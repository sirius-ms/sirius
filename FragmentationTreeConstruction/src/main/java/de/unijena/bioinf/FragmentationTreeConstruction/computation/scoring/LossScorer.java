package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.FragmentationTreeConstruction.inspection.Inspectable;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * @author Kai Dührkop
 */
public interface LossScorer {

    public Object prepare(ProcessedInput inputh);

    public double score(Loss loss, ProcessedInput input, Object precomputed);

}
