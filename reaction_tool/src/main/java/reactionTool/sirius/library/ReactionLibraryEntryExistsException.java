package reactionTool.sirius.library;

/**
 * Raised when adding a reaction or reaction sequence whose name (case-insensitively) already exists.
 * Maps naturally to a "conflict" on a web boundary.
 */
public class ReactionLibraryEntryExistsException extends ReactionLibraryException {
    public ReactionLibraryEntryExistsException(String entityType, String name) {
        super(entityType + " with name '" + name + "' already exists.");
    }
}
