package reactionTool.sirius.library;

/**
 * Base type for all business-rule violations raised by the {@link ReactionLibrary}. These are plain,
 * framework-independent exceptions: it is up to the calling layer (e.g. a web adapter) to translate
 * them into protocol-specific responses such as HTTP status codes.
 */
public abstract class ReactionLibraryException extends RuntimeException {
    protected ReactionLibraryException(String message) {
        super(message);
    }
}
