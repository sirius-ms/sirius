package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.legacy;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationPathway;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * @author Kai Dührkop
 */
public abstract class AbstractEdgeScorer implements LossScorer {
    @Override
    public Object prepare(ProcessedInput input, FragmentationPathway graph) {
        return null;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        return score(loss, input);
    }

    public abstract double score(Loss loss, ProcessedInput input);
}
