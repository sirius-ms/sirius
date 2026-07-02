package de.unijena.bioinf.ms.middleware.model.reactions;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API (wire) model for a single reaction in the reaction library. Kept separate from the persistence
 * domain model ({@link reactionTool.sirius.model.Reaction}) so the public contract is decoupled from
 * how reactions are stored; {@code ReactionServiceImpl} maps between the two.
 */
public class Reaction {
    private String name;
    private String smarts;
    /**
     * Whether this reaction is one of the defaults shipped with the application as opposed to one a
     * user created. Server-controlled and read-only: it is filled in on responses and ignored on
     * input, so a client can never mark its own reaction as preshipped.
     */
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private boolean preshipped;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSmarts() {
        return smarts;
    }

    public void setSmarts(String smarts) {
        this.smarts = smarts;
    }

    public boolean isPreshipped() {
        return preshipped;
    }

    public void setPreshipped(boolean preshipped) {
        this.preshipped = preshipped;
    }
}
