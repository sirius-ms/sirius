package de.unijena.bioinf.GibbsSampling.model.scorer;

import de.unijena.bioinf.GibbsSampling.model.FragmentWithIndex;
import de.unijena.bioinf.GibbsSampling.model.ScoredFragment;

public class CommonFragmentAndLossWithTreeScoresScorer extends CommonFragmentAndLossScorer{

    protected  double MINIMUM_NUMBER_MATCHED_PEAKS_LOSSES = 0.1; //changed from 5

    public CommonFragmentAndLossWithTreeScoresScorer(double threshold) {
        super(threshold);
    }

    @Override
    protected double scoreMatchedPeaks(PeakWithExplanation peak1, PeakWithExplanation peak2){
        return Math.max(0, peak1.bestScore+peak2.bestScore);
    }

    @Override
    protected double scoreMatchedFragments(FragmentWithIndex fragment1, FragmentWithIndex fragment2){
        return Math.max(0, fragment1.getScore()+fragment2.getScore());
    }
}
