package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.confidence_score.ExpansiveSearchConfidenceMode;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StructureSearchResult implements ResultAnnotation {

    private final double confidenceScore;
    private final double confidencScoreApproximate;
    private final int mcesIndex;
    private final ExpansiveSearchConfidenceMode.Mode expansiveSearchConfidenceMode;


    public static StructureSearchResult of(ConfidenceResult confidenceResult, ExpansiveSearchConfidenceMode.Mode expansiveSearchConfidenceMode) {
        return StructureSearchResult.builder()
                .confidenceScore(confidenceResult.score.score())
                .confidencScoreApproximate(confidenceResult.scoreApproximate.score())
                .mcesIndex(confidenceResult.mcesIndex)
                .expansiveSearchConfidenceMode(expansiveSearchConfidenceMode)
                .build();
    }
}
