package de.unijena.bioinf.ms.middleware.service.reactions;

import reactionTool.sirius.library.ReactionLibrary;
import reactionTool.sirius.model.Reaction;
import reactionTool.sirius.model.ReactionSequence;
import reactionTool.sirius.model.ParallelStep;
import reactionTool.sirius.model.LoopStep;
import reactionTool.sirius.model.Step;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReactionSequenceServiceTest {

    @Test
    void testReactionSequenceServiceWithTempFile(@TempDir Path tempDir) throws IOException {
        Path tempDbFile = tempDir.resolve("reactions.db");

        // Reactions and sequences share a single library (one database file)
        ReactionLibrary reactionLibrary = new ReactionLibrary(tempDbFile);
        ReactionServiceImpl reactionService = new ReactionServiceImpl(reactionLibrary);
        ReactionSequenceServiceImpl sequenceService = new ReactionSequenceServiceImpl(reactionLibrary);

        try {
            // Initially empty sequences
            List<ReactionSequence> sequences = sequenceService.getSequences();
            assertTrue(sequences.isEmpty());

            // Construct a ReactionSequence using internal model classes
            ReactionSequence sequence1 = new ReactionSequence();
            sequence1.setSequenceName("TestWorkFlow");

            List<Step> steps = new ArrayList<>();
            ParallelStep parallelStep = new ParallelStep();
            List<Reaction> reactions = new ArrayList<>();
            Reaction reaction = new Reaction();
            reaction.setName("GAME1 (Glycosylation)");
            reaction.setSmarts("[C:1](-[OH:2])>>[C:1](-[O:2]C3OC(CO)C(O)C(O)C3O)");
            reactions.add(reaction);
            parallelStep.setReactions(reactions);
            steps.add(parallelStep);

            LoopStep loopStep = new LoopStep();
            loopStep.setIterations(3);
            List<Step> loopSteps = new ArrayList<>();
            loopSteps.add(parallelStep);
            loopStep.setSteps(loopSteps);
            steps.add(loopStep);

            sequence1.setSteps(steps);

            // Add to library - should automatically register "GAME1 (Glycosylation)" into the reaction library on-the-fly
            sequenceService.addSequence(sequence1);

            // 1. Verify reaction was registered in the reaction library
            de.unijena.bioinf.ms.middleware.model.reactions.Reaction libReaction = reactionService.getReaction("GAME1 (Glycosylation)");
            assertNotNull(libReaction);
            assertEquals("GAME1 (Glycosylation)", libReaction.getName());
            assertEquals("[C:1](-[OH:2])>>[C:1](-[O:2]C3OC(CO)C(O)C(O)C3O)", libReaction.getSmarts());

            // 2. Verify sequence was saved and retrieved successfully
            sequences = sequenceService.getSequences();
            assertEquals(1, sequences.size());
            assertEquals("TestWorkFlow", sequences.get(0).getSequenceName());
            assertEquals(2, sequences.get(0).getSteps().size());

            ParallelStep retrievedParallel = (ParallelStep) sequences.get(0).getSteps().get(0);
            assertEquals(1, retrievedParallel.getReactions().size());
            assertEquals("GAME1 (Glycosylation)", retrievedParallel.getReactions().get(0).getName());
            // 3. Verify SMARTS pattern was successfully resolved/dereferenced on-the-fly!
            assertEquals("[C:1](-[OH:2])>>[C:1](-[O:2]C3OC(CO)C(O)C(O)C3O)", retrievedParallel.getReactions().get(0).getSmarts());

            // 4. Test submitting a sequence with a non-existent reaction and NO smarts (should fail with 400 Bad Request)
            ReactionSequence sequenceInvalid = new ReactionSequence();
            sequenceInvalid.setSequenceName("InvalidWorkflow");
            List<Step> invalidSteps = new ArrayList<>();
            ParallelStep invalidParallel = new ParallelStep();
            List<Reaction> invalidReactions = new ArrayList<>();
            Reaction invalidRx = new Reaction();
            invalidRx.setName("Non-existent Reaction"); // Does not exist and smarts is null
            invalidReactions.add(invalidRx);
            invalidParallel.setReactions(invalidReactions);
            invalidSteps.add(invalidParallel);
            sequenceInvalid.setSteps(invalidSteps);

            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                sequenceService.addSequence(sequenceInvalid);
            });

            // Get single sequence by name
            ReactionSequence retrieved = sequenceService.getSequence("TestWorkFlow");
            assertNotNull(retrieved);
            assertEquals("TestWorkFlow", retrieved.getSequenceName());

            // Get single sequence by name (case-insensitive)
            ReactionSequence retrievedCase = sequenceService.getSequence("testworkflow");
            assertNotNull(retrievedCase);

            // Get non-existent sequence (should fail)
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                sequenceService.getSequence("Non-existent");
            });

            // Add a duplicate name (should fail)
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                sequenceService.addSequence(sequence1);
            });

            // Deleting a reaction that is still referenced by a sequence must be rejected (409 Conflict)
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                reactionService.deleteReaction("GAME1 (Glycosylation)");
            });

            // Delete a sequence (case-insensitive)
            sequenceService.deleteSequence("TESTWORKFLOW");
            sequences = sequenceService.getSequences();
            assertTrue(sequences.isEmpty());

            // With the sequence gone, the reaction is no longer referenced and can be deleted
            reactionService.deleteReaction("GAME1 (Glycosylation)");

            // Delete non-existent (should fail)
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                sequenceService.deleteSequence("TESTWORKFLOW");
            });

        } finally {
            // Clean up and close the shared library
            reactionLibrary.close();
        }
    }
}
