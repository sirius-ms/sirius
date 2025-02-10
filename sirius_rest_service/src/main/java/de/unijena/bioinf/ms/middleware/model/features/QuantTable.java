package de.unijena.bioinf.ms.middleware.model.features;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "QuantTableExperimental",
        description = "EXPERIMENTAL: This schema is experimental and may be changed (or even removed) without notice until it is declared stable.")
public class QuantTable {

    protected QuantMeasure quantificationMeasure;
    protected QuantRowType rowType;

    @Schema(nullable = true) protected long[] rowIds;
    @Schema(nullable = true) protected long[] columnIds;
    @Schema(nullable = true) protected String[] rowNames;
    @Schema(nullable = true) protected String[] columnNames;
    private double[][] values;
}
