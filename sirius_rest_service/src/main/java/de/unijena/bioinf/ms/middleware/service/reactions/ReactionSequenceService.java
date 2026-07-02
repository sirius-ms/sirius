package de.unijena.bioinf.ms.middleware.service.reactions;

import reactionTool.sirius.model.ReactionSequence;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.List;

public interface ReactionSequenceService {
    @NotNull
    List<ReactionSequence> getSequences() throws IOException;
    void addSequence(@NotNull ReactionSequence sequence) throws IOException;
    void deleteSequence(@NotNull String name) throws IOException;
    @NotNull
    ReactionSequence getSequence(@NotNull String name) throws IOException;
}
