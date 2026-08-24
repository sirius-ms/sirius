package de.unijena.bioinf.lcms.align;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The order candidate alignments are considered in has to be a function of the candidates alone.
 * <p>
 * The greedy pass walks them best first and claims each mass of interest once, so of two equally good alignments the
 * one that sorts first is taken and the other is dropped. Ordering by score alone leaves that decision to the order
 * the candidates were generated in, which follows the alignment storage - and the uid an alignment storage hands out
 * comes from a counter incremented by parallel jobs, so it is not the same order twice. Two runs on identical input
 * then align different peaks, which is measurable downstream: an identical configuration run twice differed by 29 of
 * about 10300 aligned features.
 */
public class GreedyAlgorithmOrderTest {

    @Test
    public void equallyGoodAlignmentsAreOrderedByTheirIndices() {
        final List<GreedyAlgorithm.PossibleAlignment> candidates = new ArrayList<>(List.of(
                new GreedyAlgorithm.PossibleAlignment(7, 3, 0.5f),
                new GreedyAlgorithm.PossibleAlignment(2, 9, 0.5f),
                new GreedyAlgorithm.PossibleAlignment(2, 4, 0.5f),
                new GreedyAlgorithm.PossibleAlignment(5, 1, 0.9f)));

        // whatever order they are offered in, sorting has to produce the same sequence
        final List<String> expected = sortedLabels(candidates);
        for (int seed = 0; seed < 5; ++seed) {
            final List<GreedyAlgorithm.PossibleAlignment> shuffled = new ArrayList<>(candidates);
            Collections.rotate(shuffled, seed);
            assertEquals(expected, sortedLabels(shuffled), "the order may not depend on how they arrived");
        }
        // best score first, then by left index, then by right
        assertEquals(List.of("5/1", "2/4", "2/9", "7/3"), expected);
    }

    private static List<String> sortedLabels(List<GreedyAlgorithm.PossibleAlignment> candidates) {
        final List<GreedyAlgorithm.PossibleAlignment> copy = new ArrayList<>(candidates);
        copy.sort(null);
        final List<String> labels = new ArrayList<>(copy.size());
        for (GreedyAlgorithm.PossibleAlignment a : copy) labels.add(a.left + "/" + a.right);
        return labels;
    }
}
