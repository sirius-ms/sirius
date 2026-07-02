package de.unijena.bioinf.ms.middleware.service.reactions;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactionTool.sirius.library.ReactionLibrary;
import reactionTool.sirius.library.ReactionLibraryException;
import reactionTool.sirius.model.ReactionSequence;

import java.io.IOException;
import java.util.List;

/**
 * Thin Spring adapter over the framework-independent {@link ReactionLibrary}. Reaction sequences are
 * exposed using the library's domain model directly, so this only delegates and translates the
 * library's domain exceptions into HTTP responses. All persistence and business logic lives in the
 * library.
 */
@Service
public class ReactionSequenceServiceImpl implements ReactionSequenceService {

    private final ReactionLibrary reactionLibrary;

    @Autowired
    public ReactionSequenceServiceImpl(ReactionLibrary reactionLibrary) {
        this.reactionLibrary = reactionLibrary;
    }

    @Override
    @NotNull
    public List<ReactionSequence> getSequences() throws IOException {
        try {
            return reactionLibrary.getSequences();
        } catch (ReactionLibraryException e) {
            throw ReactionExceptions.toResponseStatus(e);
        }
    }

    @Override
    public void addSequence(@NotNull ReactionSequence sequence) throws IOException {
        try {
            reactionLibrary.addSequence(sequence);
        } catch (ReactionLibraryException e) {
            throw ReactionExceptions.toResponseStatus(e);
        }
    }

    @Override
    public void deleteSequence(@NotNull String name) throws IOException {
        try {
            reactionLibrary.deleteSequence(name);
        } catch (ReactionLibraryException e) {
            throw ReactionExceptions.toResponseStatus(e);
        }
    }

    @Override
    @NotNull
    public ReactionSequence getSequence(@NotNull String name) throws IOException {
        try {
            return reactionLibrary.getSequence(name);
        } catch (ReactionLibraryException e) {
            throw ReactionExceptions.toResponseStatus(e);
        }
    }
}
