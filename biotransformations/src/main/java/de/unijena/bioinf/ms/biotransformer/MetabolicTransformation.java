package de.unijena.bioinf.ms.biotransformer;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.EnumSet;

@Getter
@AllArgsConstructor
public enum MetabolicTransformation {
    PHASE_1_CYP450("Phase I (CYP450)", true, 3),
    EC_BASED("EC-Based", false, 3),
    PHASE_2("Phase II", false, 1),
    HUMAN_GUT("Human Gut Microbial", false, 3),
    ENV_MICROBIAL("Environmental Microbial", false, 1),
    ALL_HUMAN("Human and Human Gut Microbial (AllHuman)", true, 4, false),
    ABIOTIC("Abiotic (AbioticBio)", true, 3, false),
    HUMAN_CUSTOM_MULTI("Custom Human Multi-Step (MultiBio)", true, 1, false);

    private static final EnumSet<MetabolicTransformation> SEQUENCE_TRANSFORMATIONS =
           EnumSet.copyOf(Arrays.stream(values()).filter(MetabolicTransformation::isSequenceAllowed).toList());

    public static EnumSet<MetabolicTransformation> valueSequenceOnly(){
        return SEQUENCE_TRANSFORMATIONS;
    }

    private static final EnumSet<MetabolicTransformation> SINGLE_TRANSFORMATION =
            EnumSet.of(PHASE_1_CYP450, EC_BASED, PHASE_2, HUMAN_GUT, ENV_MICROBIAL, ALL_HUMAN, ABIOTIC);

    public static EnumSet<MetabolicTransformation> valueSingleOnly(){
        return SINGLE_TRANSFORMATION;
    }

    private final String displayName;
    private final boolean cypMode;
    private final int minIterations;
    private final int maxIterations;
    private final boolean sequenceAllowed;


    MetabolicTransformation(String displayName, boolean cypMode, int maxIterations) {
        this(displayName, cypMode, 1, maxIterations, true);
    }

    MetabolicTransformation(String displayName, boolean cypMode, int maxIterations, boolean sequenceAllowed) {
        this(displayName, cypMode, 1, maxIterations, sequenceAllowed);
    }
}
