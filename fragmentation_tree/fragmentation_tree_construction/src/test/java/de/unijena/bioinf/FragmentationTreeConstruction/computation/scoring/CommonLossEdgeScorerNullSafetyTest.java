package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase-1 (H3): after the LossSizeScorer refactor, {@link CommonLossEdgeScorer} folds a
 * {@link LossSizeScorer} into itself and dereferences it while scoring. If a profile registers a
 * {@code CommonLossEdgeScorer} without a {@code LossSizeScorer} to fold in, scoring must fail with a
 * clear, actionable error instead of a cryptic {@link NullPointerException}.
 */
public class CommonLossEdgeScorerNullSafetyTest {

    private static final MolecularFormula LOSS = MolecularFormula.parseOrThrow("C2H4");

    @Test
    public void scoringWithoutLossSizeScorerFailsClearly_H3() {
        CommonLossEdgeScorer scorer = new CommonLossEdgeScorer();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> scorer.score(LOSS),
                "scoring without a LossSizeScorer must throw a clear IllegalStateException, not an NPE (H3)");
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().toLowerCase().contains("losssizescorer"),
                "the exception message should name the missing LossSizeScorer, was: " + ex.getMessage());
    }

    @Test
    public void scoringWithLossSizeScorerWorks_H3() {
        CommonLossEdgeScorer scorer = new CommonLossEdgeScorer();
        scorer.setLossSizeScorer(new LossSizeScorer());
        assertDoesNotThrow(() -> scorer.score(LOSS),
                "scoring must work once a LossSizeScorer is wired");
    }
}
