package de.unijena.bioinf.fingerid;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.unijena.bioinf.confidence_score.ExpansiveSearchConfidenceMode;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder

public class StructureSearchResult implements ResultAnnotation {


    private final double confidenceScore;
    private final double confidencScoreApproximate;
    private final ExpansiveSearchConfidenceMode.Mode expansiveSearchConfidenceMode;


    public StructureSearchResult(@JsonProperty("confidenceScore") double confidenceScore,@JsonProperty("confidenceScoreApproximate") double confidencScoreApproximate, @JsonProperty("expansiveSearchConfidenceMode") ExpansiveSearchConfidenceMode.Mode expansiveSearchConfidenceMode){
        this.confidenceScore=confidenceScore;
        this.confidencScoreApproximate=confidencScoreApproximate;
        this.expansiveSearchConfidenceMode=expansiveSearchConfidenceMode;
    }




    public static StructureSearchResult of(ConfidenceResult confidenceResult, ExpansiveSearchConfidenceMode.Mode expansiveSearchConfidenceMode) {
        return StructureSearchResult.builder()
                .confidenceScore(confidenceResult.score.score())
                .confidencScoreApproximate(confidenceResult.scoreApproximate.score())
                .expansiveSearchConfidenceMode(expansiveSearchConfidenceMode)
                .build();
    }
}
