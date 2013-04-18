package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.legacy;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationPathway;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.functional.Function;
import de.unijena.bioinf.functional.list.ListOperations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Kai Dührkop
 */
public class EdgeScoreList implements LossScorer {

    private final LossScorer[] scorers;

    public EdgeScoreList(LossScorer... scorers) {
        this.scorers = scorers;
    }
    public EdgeScoreList(Collection<LossScorer> scorers) {
        this(scorers.toArray(new LossScorer[scorers.size()]));
    }

    public List<LossScorer> getScorers() {
        return Collections.unmodifiableList(Arrays.asList(scorers));
    }

    @Override
    public Object prepare(final ProcessedInput input, final FragmentationPathway graph) {
        return ListOperations.singleton().map(Arrays.asList(scorers), new Function<LossScorer, Object>() {
            @Override
            public Object apply(LossScorer arg) {
                return arg.prepare(input, graph);
            }
        });
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final List prepared = (List)precomputed;
        double score = 0d;
        int i=0;
        for (LossScorer scorer : scorers) {
            score += scorer.score(loss, input, prepared.get(i++));
            assert !Double.isNaN(score) : scorer.getClass().getName() + " returns NaN as score for " + loss;
        }
        return score;
    }
}
