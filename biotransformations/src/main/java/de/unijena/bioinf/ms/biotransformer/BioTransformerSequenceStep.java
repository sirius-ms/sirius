package de.unijena.bioinf.ms.biotransformer;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter @Setter @Accessors(chain = true)
public class BioTransformerSequenceStep {
    private MetabolicTransformation metabolicTransformation;
    private int iterations;
}
