package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationPathway;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

public class DBELossScorer implements EdgeScorer {

    private final double score;

    public DBELossScorer(double score) {
        this.score = score;
    }

    public DBELossScorer() {
        this.score = Math.log(0.25);
    }

    @Override
    public Object prepare(ProcessedInput input, FragmentationPathway graph) {
        return null;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final int rdb = loss.getLoss().doubledRDBE();
        if (rdb < 0) return score;
        else return 0;
    }
}
