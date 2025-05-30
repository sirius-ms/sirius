package de.unijena.bioinf.lcms.align;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AlignmentThresholds {

    private Double maximalAllowedRetentionTimeError;
    private Deviation maximalAllowedMassError;

    public boolean hasRetentionTimeThreshold() {
        return maximalAllowedMassError!=null;
    }

    public boolean hasMassThreshold() {
        return maximalAllowedMassError!=null;
    }


}
