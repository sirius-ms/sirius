package reactionTool.sirius.library;

import reactionTool.sirius.model.LoopStep;
import reactionTool.sirius.model.ParallelStep;
import reactionTool.sirius.model.Reaction;
import reactionTool.sirius.model.ReactionSequence;
import reactionTool.sirius.model.Step;

import java.util.function.Consumer;

/**
 * Walks the (possibly nested) step structure of a {@link ReactionSequence} and applies an action to
 * every referenced {@link Reaction}.
 */
final class ReactionSequenceWalker {

    private ReactionSequenceWalker() {
    }

    static void forEachReaction(ReactionSequence sequence, Consumer<Reaction> action) {
        if (sequence == null || sequence.getSteps() == null) {
            return;
        }
        for (Step step : sequence.getSteps()) {
            forEachReaction(step, action);
        }
    }

    private static void forEachReaction(Step step, Consumer<Reaction> action) {
        if (step instanceof ParallelStep parallelStep) {
            if (parallelStep.getReactions() != null) {
                for (Reaction reaction : parallelStep.getReactions()) {
                    action.accept(reaction);
                }
            }
        } else if (step instanceof LoopStep loopStep) {
            if (loopStep.getSteps() != null) {
                for (Step subStep : loopStep.getSteps()) {
                    forEachReaction(subStep, action);
                }
            }
        }
    }

    /**
     * @return true if the sequence references a reaction with the given name (case-insensitive).
     */
    static boolean referencesReaction(ReactionSequence sequence, String reactionName) {
        boolean[] found = {false};
        forEachReaction(sequence, r -> {
            if (reactionName.equalsIgnoreCase(r.getName())) {
                found[0] = true;
            }
        });
        return found[0];
    }
}
