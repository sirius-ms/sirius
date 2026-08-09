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

    @Schema(enumAsRef = true, name = "QuantTableOptField", nullable = true)
    public enum OptField {none, columnNames, columnSources}

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

    /**
     * Names of the runs the objects are quantified in, in the order of {@link #columnIds}.
     * <p>
     * Optional field, only present if requested, since the table can be read by run id alone.
     * <p>
     * The name is the one the run carries in the project. It is either the sample name given when the data was
     * imported, or, if none was given, derived from the measurement itself: the run id inside the file, and only
     * failing that the file name. A name is therefore not necessarily the name of the file the run came from, and
     * it is not guaranteed to be unique. Use {@link #columnSources} to identify the input file, and
     * {@link #columnIds} to identify the run.
     */
    @Schema(nullable = true) protected String[] columnNames;

    /**
     * Files the runs were imported from, in the order of {@link #columnIds}, to relate a column back to the input
     * data. Same value as the source of the corresponding run.
     * <p>
     * Optional field, only present if requested, since it is not needed to read the table.
     */
    @Schema(nullable = true) protected String[] columnSources;

    private double[][] values;
}
