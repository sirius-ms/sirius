package de.unijena.bioinf.ms.biotransformer;

import biotransformer.btransformers.Biotransformer;
import biotransformer.utils.BiotransformerSequenceStep;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;

@Getter @Setter @Accessors(chain = true)
public class BioTransformerSettings {
    private MetabolicTransformation metabolicTransformation;
    private int iterations; // Number of iterations
    private Cyp450Mode cyp450Mode; // CYP450 Mode
    private int p2Mode; // Phase II Mode
    private boolean useDB; // Use the database flag
    private boolean useSub; // Use the substructure flag

    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.NONE)
    private ArrayList<BiotransformerSequenceStep> sequenceSteps = null;

    public void addSequenceStep(MetabolicTransformation transformation, int iterations) {
        if (sequenceSteps == null)
            sequenceSteps = new ArrayList<>();
        sequenceSteps.add(new BiotransformerSequenceStep(toBType(transformation), iterations));
    }

    private static Biotransformer.bType toBType(MetabolicTransformation metabolicTransformation) {
        switch (metabolicTransformation){
            case PHASE_1_CYP450 -> {return Biotransformer.bType.CYP450;}
            case EC_BASED -> {return Biotransformer.bType.ECBASED;}
            case ENV_MICROBIAL -> {return Biotransformer.bType.ENV;}
            case HUMAN_GUT -> {return Biotransformer.bType.HGUT;}
            case PHASE_2 -> { return Biotransformer.bType.PHASEII;}
        }
        throw new IllegalArgumentException("Unsupported metabolic transformation: " + metabolicTransformation);
    }
}
