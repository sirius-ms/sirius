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

        // R1: Chlorination (C -> CCl)
        Reaction r1 = new Reaction();
        r1.setName("R1");
        r1.setSmarts("[C:1][H]>>[C:1]Cl");

        // R2: Hydrolysis (CCl -> CO)
        Reaction r2 = new Reaction();
        r2.setName("R2");
        r2.setSmarts("[C:1]Cl>>[C:1][OH]");

        ParallelStep step1 = new ParallelStep();
        step1.setReactions(List.of(r1));

        ParallelStep step2 = new ParallelStep();
        step2.setReactions(List.of(r2));

        ReactionSequence sequence = new ReactionSequence();
        sequence.setSteps(Arrays.asList(step1, step2));

        List<String> initialSmiles = List.of("C");
        List<String> results = handler.process(sequence, initialSmiles);

        System.out.println("Retention Results: " + results);

        boolean hasChloromethane = results.stream().anyMatch(s -> s.contains("CCl") || s.contains("ClC"));
        boolean hasMethanol = results.stream().anyMatch(s -> s.contains("CO") || s.contains("OC"));

        assertTrue(hasMethanol, "Should have methanol (final product)");
        assertTrue(hasChloromethane, "Should have chloromethane (intermediate)");
    }

    @Test
    public void testProductSplitting() {
        ReactionToolHandler handler = new ReactionToolHandler(null, null);

        // R1: Ether Cleavage (COCC -> CO + CCO)
        Reaction r1 = new Reaction();
        r1.setName("Cleavage");
        r1.setSmarts("[C:1][O:2][C:3]>>[C:1][OH].[C:3][OH]");

        ParallelStep step1 = new ParallelStep();
        step1.setReactions(List.of(r1));

        ReactionSequence sequence = new ReactionSequence();
        sequence.setSteps(List.of(step1));

        // Note: Using a simple ether for testing
        List<String> results = handler.process(sequence, List.of("COCC"));

        System.out.println("Splitting Results: " + results);

        boolean hasMethanol = results.stream().anyMatch(s -> s.contains("CO") || s.contains("OC"));
        boolean hasEthanol = results.stream().anyMatch(s -> s.contains("CCO") || s.contains("OCC"));

        assertTrue(hasMethanol, "Should have methanol fragment");
        assertTrue(hasEthanol, "Should have ethanol fragment");
    }
}
