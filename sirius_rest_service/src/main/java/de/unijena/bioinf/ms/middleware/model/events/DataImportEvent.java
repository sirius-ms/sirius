package de.unijena.bioinf.ms.middleware.model.events;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

//todo add information about traces if needed.
@Getter
@Builder
public class DataImportEvent {
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String importJobId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    List<String> importedCompoundIds;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    List<String> importedFeatureIds;
}
