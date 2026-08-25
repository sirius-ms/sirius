package de.unijena.bioinf.ms.middleware.service.reactions;

import de.unijena.bioinf.ms.middleware.model.reactions.Reaction;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.List;

public interface ReactionService {
    @NotNull
    List<Reaction> getReactions() throws IOException;
    void addReaction(@NotNull Reaction reaction) throws IOException;
    void deleteReaction(@NotNull String name) throws IOException;
    @NotNull
    Reaction getReaction(@NotNull String name) throws IOException;
}
