package de.unijena.bioinf.ms.biotransformer;

import biotransformer.transformation.Biotransformation;
import lombok.Getter;

import java.util.List;

public class BioTransformerResult {
    @Getter
    private final List<Biotransformation> biotranformations;

    public BioTransformerResult(List<Biotransformation> biotranformations) {
        this.biotranformations = biotranformations;
    }
}
