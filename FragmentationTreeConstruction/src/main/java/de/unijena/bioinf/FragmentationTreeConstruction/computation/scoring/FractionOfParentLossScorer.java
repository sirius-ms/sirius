package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.List;

/**
 * compares the mass of a loss to the parent mass or if unknown to the mass of the heaviest peak.
 * Different to RelativeLossSizeScorer because it don't punishes if loss sizes don't grow with parent mass (?)
 */
@Called("FractionOfParent")
public class FractionOfParentLossScorer implements LossScorer {
    //todo implement as PeakPairScorer?...
    @Override
    public Double prepare(ProcessedInput inputh) {
        //if ion mass known take this
        double comparableMass = (inputh.getParentPeak()==null ? 0 : inputh.getParentPeak().getMz());
        //else find largest mass, Double.MAX_VALUE means existing dummy node
        if (comparableMass==0d || comparableMass==Double.MAX_VALUE){
            comparableMass = 0d;
            final List<ProcessedPeak> peaks = inputh.getMergedPeaks();
            for (ProcessedPeak peak : peaks) {
                if (peak.getMz()>comparableMass && peak.getMz()<Double.MAX_VALUE) comparableMass = peak.getMz();
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
