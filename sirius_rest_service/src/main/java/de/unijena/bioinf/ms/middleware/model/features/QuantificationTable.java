package de.unijena.bioinf.ms.middleware.model.features;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantificationMeasure;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "QuantificationTableExperimental",
        description = "EXPERIMENTAL: This schema is experimental and may be changed (or even removed) without notice until it is declared stable.")
public class QuantificationTable {

    @Schema(name = "QuantificationRowType")
    public enum RowType {
        FEATURES,
        COMPOUNDS
    }

    @Schema(name = "QuantificationColumnType")
    public enum ColumnType {
        // the only supported column type at the moment
        SAMPLES
    }

    private QuantificationMeasure quantificationMeasure;
    private RowType rowType;
    private ColumnType columnType;

    @Schema(nullable = true) private long[] rowIds;
    @Schema(nullable = true) private long[] columnIds;
    @Schema(nullable = true) private String[] rowNames;
    @Schema(nullable = true) private String[] columnNames;
    private double[][] values;
}
