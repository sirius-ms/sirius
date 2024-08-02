package de.unijena.bioinf.cmlFragmentation;

import de.unijena.bioinf.fragmenter.*;
import org.openscience.cdk.interfaces.IBond;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

public class FragStepDependentScoring implements CombinatorialFragmenterScoring{

    // FragStepProbabilities[0] = 1d has several reasons:
    // 1. we can write (FragStepProbabilities[t+1] / FragStepProbabilities[t]) without checking t > 0
    // 2. it's the probability that a peak occurs with 0 fragmentation steps --> the precursor peak
    private static final double[] FragStepProbabilities = new double[]{1d, 0.23200506539624902, 0.29780554775650075, 0.24083974067373556,
            0.15000912945467218, 0.08073845928304674, 0.03962606218012708, 0.01825269036985983, 0.007562273079982811,
            0.0030261530269075953, 0.0010547380874712867, 0.00034328739639395657, 0.00010696636264449391,
            4.228902709200913e-05, 1.1194154230237714e-05, 4.975179657883428e-06 };

    private final HashMap<BitSet, Double> fragment2BondScoresSum;
    private final CombinatorialFragmenterScoring scoring;

    public FragStepDependentScoring(CombinatorialFragmenterScoring scoring) {
        this.scoring = scoring;
        this.fragment2BondScoresSum = new HashMap<>();
    }

    @Override
    public double scoreFragment(CombinatorialNode fragment){
        return 0d;
    }

    @Override
    public double scoreBond(IBond bond, boolean direction){
        return this.scoring.scoreBond(bond, direction);
    }

    @Override
    public double scoreEdge(CombinatorialEdge edge){
        final CombinatorialNode sourceFragment = edge.getSource();
        final int sourceFragmentDepth = sourceFragment.getDepth();

        if(sourceFragmentDepth >= FragStepProbabilities.length-1) return Double.NEGATIVE_INFINITY;
        final double logFragStep = Math.log(FragStepProbabilities[sourceFragmentDepth+1]) -
                Math.log(FragStepProbabilities[sourceFragmentDepth]);

        if(!this.fragment2BondScoresSum.containsKey(sourceFragment.getFragment().getBitSet())){
            final MolecularGraph molecule = sourceFragment.getFragment().getIntactMolecule();
            final ArrayList<Integer> bondsInFragment = sourceFragment.getFragment().bonds();

            double sum = 0d;
            for(final int bondIdx : bondsInFragment){
                IBond bond = molecule.getBonds()[bondIdx];
                sum += Math.exp(this.scoring.scoreBond(bond, true));
                sum += Math.exp(this.scoring.scoreBond(bond, false)); // EMFragmenterScoring2 is directed!
            }
            this.fragment2BondScoresSum.put(sourceFragment.getFragment().getBitSet(), sum);
        }

        final double bondProbSum = Math.log(this.fragment2BondScoresSum.get(sourceFragment.getFragment().getBitSet()));
        final double logBondBreak1 = this.scoring.scoreBond(edge.getCut1(), edge.getDirectionOfFirstCut()) - bondProbSum;
        final double logBondBreak2 = edge.getCut2() != null ?
                this.scoring.scoreBond(edge.getCut2(), edge.getDirectionOfSecondCut()) - bondProbSum : 0d;

        return logFragStep + logBondBreak1 + logBondBreak2;
    }

}
