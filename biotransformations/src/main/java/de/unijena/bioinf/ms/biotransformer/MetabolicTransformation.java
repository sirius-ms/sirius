package de.unijena.bioinf.ms.biotransformer;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public enum MetabolicTransformation {
    PHASE_1_CYP450("cyp450"),
    EC_BASED("ecBased"),
    PHASE_2("phase2"),
    HUMAN_GUT("gut"),
    ENV_MICROBIAL("microbio"),
    ALL_HUMAN("allHuman"),
    ABIOTIC("abiotic"),
    HUMAN_CUSTOM_MULTI("multihuman");

    private static final EnumSet<MetabolicTransformation> SEQUENCE_TRANSFORMATIONS =
           EnumSet.of( PHASE_1_CYP450, EC_BASED, PHASE_2, HUMAN_GUT, ENV_MICROBIAL);

    public static EnumSet<MetabolicTransformation> valueSequenceOnly(){
        return SEQUENCE_TRANSFORMATIONS;
    }

    private static final EnumSet<MetabolicTransformation> SINGLE_TRANSFORMATION =
           EnumSet.of(PHASE_1_CYP450, EC_BASED, PHASE_2, HUMAN_GUT, ENV_MICROBIAL, ALL_HUMAN, ABIOTIC);

    public static EnumSet<MetabolicTransformation> valueSingleOnly(){
        return SINGLE_TRANSFORMATION;
    }

    @Getter
    private final String displayName;

    MetabolicTransformation(@NotNull String displayName) {
        this.displayName = displayName;
    }
}
