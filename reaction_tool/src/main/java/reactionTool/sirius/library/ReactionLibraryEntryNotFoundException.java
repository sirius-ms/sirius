package reactionTool.sirius.library;

/**
 * Raised when a requested reaction or reaction sequence does not exist. Maps naturally to a
 * "not found" on a web boundary.
 */
public class ReactionLibraryEntryNotFoundException extends ReactionLibraryException {
    public ReactionLibraryEntryNotFoundException(String entityType, String name) {
        super(entityType + " with name '" + name + "' not found.");
    }
}
