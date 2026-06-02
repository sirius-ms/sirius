package reactionTool.sirius;

import org.junit.jupiter.api.Test;
import reactionTool.sirius.model.ParallelStep;
import reactionTool.sirius.model.Reaction;
import reactionTool.sirius.model.ReactionSequence;
import reactionTool.sirius.model.Step;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReactionToolHandlerTest {

    @Test
    public void testIntermediateRetention() {
        ReactionToolHandler handler = new ReactionToolHandler(null, null);

        // R1: C -> CO
        Reaction r1 = new Reaction();
        r1.setName("R1");
        r1.setSmarts("[C:1]>>[C:1][O:2]");

        // R2: CO -> C=O
        Reaction r2 = new Reaction();
        r2.setName("R2");
        r2.setSmarts("[C:1][O:2]>>[C:1]=[O:2]");

        ParallelStep step1 = new ParallelStep();
        step1.setReactions(List.of(r1));

        ParallelStep step2 = new ParallelStep();
        step2.setReactions(List.of(r2));

        ReactionSequence sequence = new ReactionSequence();
        sequence.setSteps(Arrays.asList(step1, step2));

        List<String> initialSmiles = List.of("C");
        List<String> results = handler.process(sequence, initialSmiles);

        System.out.println("Results: " + results);

        // Currently, we expect "CO" to be missing if it reacted in step 2
        // We want both "CO" and "C=O" (and maybe "C")
        
        boolean hasMethanol = results.stream().anyMatch(s -> s.contains("CO") || s.contains("OC"));
        boolean hasFormaldehyde = results.stream().anyMatch(s -> s.contains("C=O") || s.contains("O=C"));

        assertTrue(hasFormaldehyde, "Should have formaldehyde (final product)");
        assertTrue(hasMethanol, "Should have methanol (intermediate)");
    }
}
