package reactionTool.sirius.library;

/**
 * Raised when caller-supplied input is invalid (e.g. a blank reaction or sequence name, or a missing
 * smarts pattern). Maps naturally to a "bad request" on a web boundary.
 */
public class InvalidReactionLibraryInputException extends ReactionLibraryException {
    public InvalidReactionLibraryInputException(String message) {
        super(message);
    }
}
