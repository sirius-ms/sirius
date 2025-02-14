package de.unijena.bioinf.ms.biotransformer;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public enum MetabolicTransformation {
    PHASE_1_CYP450(""),
    EC_BASED(""),
    PHASE_2(""),
    HUMAN_GUT(""),
    ENV_MICROBIAL(""),
    ALL_HUMAN(""),
    SUPER_BIO(""),
    ABIOTIC(""),
    HUMAN_CUSTOM_MULTI("");


    @Getter
    private final String displayName;

    MetabolicTransformation(@NotNull String displayName) {
        this.displayName = displayName;
    }
}
