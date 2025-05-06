package de.unijena.bioinf.ms.biotransformer;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public enum Cyp450Mode {
    RULE_BASED("Rule Based"),
    CY_PRODUCT("CyProduct"),
    COMBINED("Combined");

    @Getter
    private final String displayName;

    Cyp450Mode(@NotNull String displayName) {
        this.displayName = displayName;
    }

    public int getCyp450ModeOrdinal(){
        return this.ordinal() + 1;
    }
}
