package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Marcus
 * Date: 25.06.13
 * Time: 00:24
 * To change this template use File | Settings | File Templates.
 */
@Called("Fraction")
public class FractionOfParentLossScorer implements LossScorer {
    @Override
    public Double prepare(ProcessedInput inputh) {
        //if ion mass known take this
        double comparableMass = (inputh.getParentPeak()==null ? 0 : inputh.getParentPeak().getMz());
        //else find largest mass
        if (comparableMass==0d){
            final List<ProcessedPeak> peaks = inputh.getMergedPeaks();
            for (ProcessedPeak peak : peaks) {
                if (peak.getMz()>comparableMass) comparableMass = peak.getMz();
            }
        }
        return comparableMass;
    }


    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        double comparableMass = (Double)precomputed;
        // Score with fraction of the parentmass or largest mass in spectrum.
        return Math.log(1-(loss.getFormula().getMass()/comparableMass));
    }
}
