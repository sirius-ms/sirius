package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.legacy;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.PeakPairScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PeakPairScoreList implements PeakPairScorer {

    private final PeakPairScorer[] scorers;

    public PeakPairScoreList(PeakPairScorer... scorers) {
        this.scorers = scorers;
    }
    public PeakPairScoreList(Collection<PeakPairScorer> scorers) {
        this(scorers.toArray(new PeakPairScorer[scorers.size()]));
    }

    public List<PeakPairScorer> getScorers() {
        return Collections.unmodifiableList(Arrays.asList(scorers));
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[][] scores) {
       for (PeakPairScorer scorer : scorers) {
        scorer.score(peaks, input, scores);
       }
    }
}
