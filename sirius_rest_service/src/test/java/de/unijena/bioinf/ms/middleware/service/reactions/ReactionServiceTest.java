package de.unijena.bioinf.ms.middleware.service.reactions;

import de.unijena.bioinf.ms.middleware.model.reactions.Reaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactionTool.sirius.library.ReactionLibrary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReactionServiceTest {

    @Test
    void testReactionServiceWithTempFile(@TempDir Path tempDir) throws IOException {
        Path tempDbFile = tempDir.resolve("reactions.db");

        // The library takes the database file location as a constructor argument.
        ReactionLibrary reactionLibrary = new ReactionLibrary(tempDbFile);
        ReactionServiceImpl service = new ReactionServiceImpl(reactionLibrary);

        try {
            // Initially, the temp database doesn't exist, so when first requested,
            // it will be created and populated from classpath reactionLibrary.json
            List<Reaction> reactions = service.getReactions();
            assertFalse(reactions.isEmpty(), "Should be populated with default reactions from reactionLibrary.json");
            assertTrue(reactions.stream().allMatch(Reaction::isPreshipped),
                    "Default reactions seeded from reactionLibrary.json must be flagged preshipped");

            int initialSize = reactions.size();

            // Add a new unique reaction. The client claims preshipped=true, which must be ignored:
            // preshipped is server-controlled and a user-added reaction is always stored as user-created.
            Reaction reaction1 = new Reaction();
            reaction1.setName("GAME 42");
            reaction1.setSmarts("[C:1]>>[C:1]");
            reaction1.setPreshipped(true);
            service.addReaction(reaction1);

            // Retrieve and check
            reactions = service.getReactions();
            assertEquals(initialSize + 1, reactions.size());

            Reaction retrieved = service.getReaction("GAME 42");
            assertNotNull(retrieved);
            assertEquals("GAME 42", retrieved.getName());
            assertEquals("[C:1]>>[C:1]", retrieved.getSmarts());
            assertFalse(retrieved.isPreshipped(),
                    "A user-added reaction must be user-created regardless of the submitted preshipped value");

            // Add duplicate (should fail)
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                service.addReaction(reaction1);
            });

            // Delete reaction
            service.deleteReaction("GAME 42");
            reactions = service.getReactions();
            assertEquals(initialSize, reactions.size());

            // Delete non-existent (should fail)
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                service.deleteReaction("GAME 42");
            });

        } finally {
            // Clean up and close db
            reactionLibrary.close();
        }

        // Once shut down, the database must not silently reopen
        assertThrows(IOException.class, service::getReactions);
    }
}
