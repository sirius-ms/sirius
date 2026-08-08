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
@Schema(description = "Quantification of features or compounds within the runs they have been detected in. " +
        "Rows refer to the quantified objects, columns to the runs. Values that could not be quantified are NaN.")
public class QuantTable {

    protected QuantMeasure quantificationMeasure;
    protected QuantRowType rowType;

    /**
     * Ids of the quantified objects, features or compounds depending on the row type.
     */
    @Schema(nullable = true) protected String[] rowIds;
    /**
     * Ids of the runs the objects are quantified in.
     */
    @Schema(nullable = true) protected String[] columnIds;
    @Schema(nullable = true) protected String[] columnNames;
    private double[][] values;
}
