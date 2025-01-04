package de.unijena.bioinf.ms.persistence.model.core.networks;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import jakarta.annotation.Nullable;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AdductEdge {
    private long leftFeatureId, rightFeatureId;
    private float mergedCorrelation, representativeCorrelation, ms2cosine, pvalue, intensityRatioScore;
    private String label;

}
