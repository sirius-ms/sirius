package de.unijena.bioinf.fingerid;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.confidence_score.ExpansiveSearchConfidenceMode;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
@Builder

public class StructureSearchResult implements ResultAnnotation {


    private final double confidenceScore;
    private final double confidencScoreApproximate;
    private final ExpansiveSearchConfidenceMode.Mode expansiveSearchConfidenceMode;
    private final List<CustomDataSources.Source> specifiedSearchDatabases;
    private final List<CustomDataSources.Source> expandedSearchDatabases;


    public StructureSearchResult(
            @JsonProperty("confidenceScore") double confidenceScore,
            @JsonProperty("confidenceScoreApproximate") double confidencScoreApproximate,
            @JsonProperty("expansiveSearchConfidenceMode") ExpansiveSearchConfidenceMode.Mode expansiveSearchConfidenceMode,
            @JsonProperty("specifiedSearchDatabases") List<CustomDataSources.Source> specifiedSearchDatabases,
            @JsonProperty("expandedSearchDatabases") List<CustomDataSources.Source> expandedSearchDatabases

    ){
        this.confidenceScore=confidenceScore;
        this.confidencScoreApproximate=confidencScoreApproximate;
        this.expansiveSearchConfidenceMode=expansiveSearchConfidenceMode;
        this.specifiedSearchDatabases=specifiedSearchDatabases;
        this.expandedSearchDatabases=expandedSearchDatabases;
    }




    public static StructureSearchResult of(@NotNull ConfidenceResult confidenceResult,
                                           @NotNull ExpansiveSearchConfidenceMode.Mode expansiveSearchConfidenceMode,
                                           @NotNull List<CustomDataSources.Source> specifiedSearchDatabases,
                                           @NotNull List<CustomDataSources.Source> expandedSearchDatabases
    ) {
        return StructureSearchResult.builder()
                .confidenceScore(confidenceResult.score.score())
                .confidencScoreApproximate(confidenceResult.scoreApproximate.score())
                .expansiveSearchConfidenceMode(expansiveSearchConfidenceMode)
                .specifiedSearchDatabases(specifiedSearchDatabases)
                .expandedSearchDatabases(expandedSearchDatabases)
                .build();
    }
}
