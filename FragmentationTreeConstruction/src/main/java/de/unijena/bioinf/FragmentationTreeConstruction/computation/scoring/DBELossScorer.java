package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationPathway;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * Created with IntelliJ IDEA.
 * User: kaidu
 * Date: 25.04.13
 * Time: 03:52
 * To change this template use File | Settings | File Templates.
 */
public class DBELossScorer implements LossScorer {

    private double score = score = Math.log(0.25);

    @Override
    public Object prepare(ProcessedInput input, FragmentationPathway graph) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final int rdb = loss.getLoss().doubledRDBE();
        if (rdb < 0) return score;
        else return 0;
    }
}
