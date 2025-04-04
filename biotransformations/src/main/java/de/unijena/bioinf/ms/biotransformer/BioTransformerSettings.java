package de.unijena.bioinf.ms.biotransformer;

import biotransformer.utils.BiotransformerSequenceStep;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;

@Getter
@Builder
public class BioTransformerSettings {
    private final MetabolicTransformation metabolicTransformation;
    private final int iterations; // Number of iterations
    private final Cyp450Mode cyp450Mode; // CYP450 Mode
    private final int p2Mode; // Phase II Mode
    private final boolean useDB; // Use the database flag
    private final boolean useSub; // Use the substructure flag
    private final ArrayList<BiotransformerSequenceStep> sequenceSteps;
}
