package de.unijena.bioinf.ms.biotransformer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter @Accessors(chain = true)
public class BioTransformerSettings {
    private MetabolicTransformation metabolicTransformation;
    private int iterations; // Number of iterations
    private Cyp450Mode cyp450Mode; // CYP450 Mode
    private P2Mode p2Mode; // Phase II Mode
    private boolean useDB; // Use the database flag
    private boolean useSub; // Use the substructure flag

    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.NONE)
    private List<BioTransformerSequenceStep> sequenceSteps = null;

    @JsonIgnore
    public void addSequenceStep(BioTransformerSequenceStep sequenceStep) {
        if (sequenceSteps == null)
            sequenceSteps = new ArrayList<>();
        sequenceSteps.add(sequenceStep);
    }

    @JsonIgnore
    public void addSequenceStep(MetabolicTransformation transformation, int iterations) {
        addSequenceStep(new BioTransformerSequenceStep().setMetabolicTransformation(transformation).setIterations(iterations));
    }
}
