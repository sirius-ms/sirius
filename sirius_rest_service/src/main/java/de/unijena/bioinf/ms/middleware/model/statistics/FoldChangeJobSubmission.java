package de.unijena.bioinf.ms.middleware.model.statistics;

import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import lombok.*;

import java.util.EnumSet;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FoldChangeJobSubmission {
    private String leftRunGroup;

    private String rightRunGroup;

    private EnumSet<AggregationType> aggregationTypes;

    private EnumSet<QuantMeasure> quantificationMeasures;

    public static FoldChangeJobSubmission of(String leftRunGroupName, String rightRunGroupName, AggregationType aggregationType, QuantMeasure quantMeasure) {
        return FoldChangeJobSubmission.builder()
                .leftRunGroup(leftRunGroupName)
                .rightRunGroup(rightRunGroupName)
                .aggregationTypes(EnumSet.of(aggregationType))
                .quantificationMeasures(EnumSet.of(quantMeasure))
                .build();
    }
}
