package de.unijena.bioinf.ms.biotransformer;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public enum MetabolicTransformation {
    PHASE_1_CYP450("cyp450"),
    EC_BASED("ecBased"),
    PHASE_2("phase2"),
    HUMAN_GUT("gut"),
    ENV_MICROBIAL("microbio"),
    ALL_HUMAN("allHuman"),
    SUPER_BIO("superbio"),
    ABIOTIC("abiotic"),
    HUMAN_CUSTOM_MULTI("multihuman");


    @Getter
    private final String displayName;

    MetabolicTransformation(@NotNull String displayName) {
        this.displayName = displayName;
    }
}
