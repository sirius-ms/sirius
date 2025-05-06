package de.unijena.bioinf.ms.biotransformer;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public enum P2Mode {
    BT_RULE_BASED("BioTransformer rules"),
    P2_RULE_ONLY("Phase2 predictor only"),
    COMBINED_RULES("Combined");


    @Getter
    private final String displayName;

    P2Mode(@NotNull String displayName) {
        this.displayName = displayName;
    }

    public int P2ModeOrdinal(){
        return this.ordinal() + 1;
    }
}
