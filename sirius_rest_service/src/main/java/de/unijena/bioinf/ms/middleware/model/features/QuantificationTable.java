package de.unijena.bioinf.ms.middleware.model.features;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuantificationTable {

    @Schema(enumAsRef = false, name = "QuantificationType", nullable = false)
    public enum QuantificationType {
        // the only supported quantification type at the moment
        APEX_HEIGHT;
    }

    @Schema(enumAsRef = false, name = "RowType", nullable = false)
    public enum RowType {
        // the only supported row type at the moment
        FEATURES;
    }

    @Schema(enumAsRef = false, name = "ColumnType", nullable = false)
    public enum ColumnType {
        // the only supported column type at the moment
        SAMPLES;
    }

    private QuantificationType quantificationType;
    private RowType rowType;
    private ColumnType columnType;

    @Schema(nullable = true) private long[] rowIds;
    @Schema(nullable = true) private long[] columnIds;
    @Schema(nullable = true) private String[] rowNames;
    @Schema(nullable = true) private String[] columnNames;
    private double[][] values;
}
