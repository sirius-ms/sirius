package de.unijena.bioinf.ms.middleware.service.reactions;

import de.unijena.bioinf.ms.middleware.model.reactions.Reaction;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactionTool.sirius.library.ReactionLibrary;
import reactionTool.sirius.library.ReactionLibraryException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin Spring adapter over the framework-independent {@link ReactionLibrary}: maps between the API
 * (wire) {@link Reaction} model and the library's domain model, and translates the library's domain
 * exceptions into HTTP responses. All persistence and business logic lives in the library.
 */
@Service
public class ReactionServiceImpl implements ReactionService {

    private final ReactionLibrary reactionLibrary;

    @Autowired
    public ReactionServiceImpl(ReactionLibrary reactionLibrary) {
        this.reactionLibrary = reactionLibrary;
    }

    @Override
    @NotNull
    public List<Reaction> getReactions() throws IOException {
        try {
            List<Reaction> result = new ArrayList<>();
            for (reactionTool.sirius.model.Reaction r : reactionLibrary.getReactions()) {
                result.add(toApi(r));
            }
            return result;
        } catch (ReactionLibraryException e) {
            throw ReactionExceptions.toResponseStatus(e);
        }
    }

    @Override
    public void addReaction(@NotNull Reaction reaction) throws IOException {
        try {
            // preshipped is server-controlled and read-only: anything a client submits is ignored, so a
            // user-added reaction is always stored as user-created (the 2-arg constructor defaults it false).
            reactionLibrary.addReaction(new reactionTool.sirius.model.Reaction(reaction.getName(), reaction.getSmarts()));
        } catch (ReactionLibraryException e) {
            throw ReactionExceptions.toResponseStatus(e);
        }
    }

    @Override
    public void deleteReaction(@NotNull String name) throws IOException {
        try {
            reactionLibrary.deleteReaction(name);
        } catch (ReactionLibraryException e) {
            throw ReactionExceptions.toResponseStatus(e);
        }
    }

    @Override
    @NotNull
    public Reaction getReaction(@NotNull String name) throws IOException {
        try {
            return toApi(reactionLibrary.getReaction(name));
        } catch (ReactionLibraryException e) {
            throw ReactionExceptions.toResponseStatus(e);
        }
    }

    private static Reaction toApi(reactionTool.sirius.model.Reaction r) {
        Reaction api = new Reaction();
        api.setName(r.getName());
        api.setSmarts(r.getSmarts());
        api.setPreshipped(r.isPreshipped());
        return api;
    }
}
