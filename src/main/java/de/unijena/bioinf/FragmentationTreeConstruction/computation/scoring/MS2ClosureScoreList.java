package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MS2ClosureScoreList implements MS2ClosureScorer {

    private final MS2ClosureScorer[] scorers;

    public MS2ClosureScoreList(MS2ClosureScorer... scorers) {
        this.scorers = scorers;
    }
    public MS2ClosureScoreList(Collection<MS2ClosureScorer> scorers) {
        this(scorers.toArray(new MS2ClosureScorer[scorers.size()]));
    }

    public List<MS2ClosureScorer> getScorers() {
        return Collections.unmodifiableList(Arrays.asList(scorers));
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[][] scores) {
       for (MS2ClosureScorer scorer : scorers) {
        scorer.score(peaks, input, scores);
       }
    }
}
