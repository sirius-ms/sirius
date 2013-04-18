package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.legacy;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DecompositionScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import de.unijena.bioinf.functional.Function;
import de.unijena.bioinf.functional.list.ListOperations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Kai Dührkop
 */
@SuppressWarnings("rawtypes")
public class VertexScoreList implements DecompositionScorer {

    private final DecompositionScorer[] scorers;

    public VertexScoreList(DecompositionScorer... scorers) {
        this.scorers = scorers;
    }
    public VertexScoreList(Collection<DecompositionScorer> scorers) {
        this(scorers.toArray(new DecompositionScorer[scorers.size()]));
    }
    
	public List<DecompositionScorer> getScorers() {
    	return Collections.unmodifiableList(Arrays.asList(scorers));
    }

    @Override
    public Object prepare(final ProcessedInput input) {
        return ListOperations.singleton().map(Arrays.asList(scorers), new Function<DecompositionScorer, Object>() {
            @Override
            public Object apply(DecompositionScorer arg) {
                return arg.prepare(input);
            }
        });
    }

    @SuppressWarnings("unchecked")
	@Override
    public double score(MolecularFormula formula, ProcessedPeak peak, ProcessedInput input, Object precomputed) {

		final List<Object> precomputeds = (List<Object>)precomputed;
        double  score = 0d;
        for (int k=0; k < scorers.length; ++k) {
            score += scorers[k].score(formula, peak, input, precomputeds.get(k));
        }
        return score;
    }
}
