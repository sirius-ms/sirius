package reactionTool.sirius.library;

import java.util.List;

/**
 * Raised when a reaction cannot be deleted because one or more sequences still reference it. Carries
 * the offending reaction name and the referencing sequence names as structured data so the calling
 * layer can format a protocol- or UI-specific message without the library needing to know about it.
 */
public class ReactionInUseException extends ReactionLibraryException {
    private final String reactionName;
    private final List<String> referencingSequences;

    public ReactionInUseException(String reactionName, List<String> referencingSequences) {
        super("Reaction with name '" + reactionName + "' is still referenced by "
                + referencingSequences.size() + " sequence(s) and cannot be deleted.");
        this.reactionName = reactionName;
        this.referencingSequences = List.copyOf(referencingSequences);
    }

    public String getReactionName() {
        return reactionName;
    }

    public List<String> getReferencingSequences() {
        return referencingSequences;
    }
}
