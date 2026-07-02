package reactionTool.sirius.library;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Raised when a submitted sequence references reactions that neither exist in the library nor carry a
 * smarts pattern that would allow registering them on the fly. Maps naturally to a "bad request" on a
 * web boundary.
 */
public class UnresolvableReactionReferencesException extends ReactionLibraryException {
    private final List<String> reactionNames;

    public UnresolvableReactionReferencesException(Collection<String> reactionNames) {
        super("Referenced reaction(s) " + new TreeSet<>(reactionNames)
                + " do not exist in the reactions library, and no smarts pattern was provided to register them.");
        this.reactionNames = List.copyOf(reactionNames);
    }

    public List<String> getReactionNames() {
        return reactionNames;
    }
}
