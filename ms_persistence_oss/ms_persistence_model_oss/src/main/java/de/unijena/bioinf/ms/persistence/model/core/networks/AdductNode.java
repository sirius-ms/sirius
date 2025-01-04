package de.unijena.bioinf.ms.persistence.model.core.networks;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AdductNode {

    private double mz;
    private long alignedFeatureId;
    private long traceId;
    private PrecursorIonType[] possibleAdducts;
    private float[] adductProbabilities;

}
