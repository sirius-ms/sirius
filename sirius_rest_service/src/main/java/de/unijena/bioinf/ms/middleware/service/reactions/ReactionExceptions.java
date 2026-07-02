package de.unijena.bioinf.ms.middleware.service.reactions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactionTool.sirius.library.ReactionInUseException;
import reactionTool.sirius.library.ReactionLibraryEntryExistsException;
import reactionTool.sirius.library.ReactionLibraryEntryNotFoundException;
import reactionTool.sirius.library.ReactionLibraryException;

import java.util.stream.Collectors;

/**
 * Translates the framework-independent {@link ReactionLibraryException} hierarchy raised by the
 * reaction library into Spring HTTP responses. This is the web-adapter seam: the library expresses
 * business-rule violations as plain exceptions, and the protocol-specific mapping lives here.
 */
final class ReactionExceptions {

    private ReactionExceptions() {
    }

    static ResponseStatusException toResponseStatus(ReactionLibraryException e) {
        if (e instanceof ReactionLibraryEntryNotFoundException) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        if (e instanceof ReactionInUseException inUse) {
            // The quoted-list wording is parsed by the web frontend to name the blocking workflows in
            // its error popup — keep the format in sync with useReactionLibrary.ts.
            String quoted = inUse.getReferencingSequences().stream()
                    .map(n -> "'" + n + "'")
                    .collect(Collectors.joining(", "));
            return new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reaction with name '" + inUse.getReactionName() + "' is still referenced by the sequence(s) " + quoted + " and cannot be deleted.");
        }
        if (e instanceof ReactionLibraryEntryExistsException) {
            return new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        // InvalidReactionLibraryInputException, UnresolvableReactionReferencesException, and any other
        // business-rule violation map to a bad request.
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
