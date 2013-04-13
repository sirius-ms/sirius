package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

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
public class EdgeScoreList implements EdgeScorer {

    private final EdgeScorer[] scorers;

    public EdgeScoreList(EdgeScorer... scorers) {
        this.scorers = scorers;
    }
    public EdgeScoreList(Collection<EdgeScorer> scorers) {
        this(scorers.toArray(new EdgeScorer[scorers.size()]));
    }

    public List<EdgeScorer> getScorers() {
        return Collections.unmodifiableList(Arrays.asList(scorers));
    }

    @Override
    public Object prepare(final ProcessedInput input, final FragmentationPathway graph) {
        return ListOperations.singleton().map(Arrays.asList(scorers), new Function<EdgeScorer, Object>() {
            @Override
            public Object apply(EdgeScorer arg) {
                return arg.prepare(input, graph);
            }
        });
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final List prepared = (List)precomputed;
        double score = 0d;
        int i=0;
        for (EdgeScorer scorer : scorers) {
            score += scorer.score(loss, input, prepared.get(i++));
            assert !Double.isNaN(score) : scorer.getClass().getName() + " returns NaN as score for " + loss;
        }
        return score;
    }
}
