package de.unijena.bioinf.ms.middleware.model.features;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.annotation.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
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

    public QuantificationTable() {
    }

    public QuantificationType getQuantificationType() {
        return quantificationType;
    }

    public void setQuantificationType(QuantificationType quantificationType) {
        this.quantificationType = quantificationType;
    }

    public RowType getRowType() {
        return rowType;
    }

    public void setRowType(RowType rowType) {
        this.rowType = rowType;
    }

    public ColumnType getColumnType() {
        return columnType;
    }

    public void setColumnType(ColumnType columnType) {
        this.columnType = columnType;
    }

    public long[] getRowIds() {
        return rowIds;
    }

    public void setRowIds(long[] rowIds) {
        this.rowIds = rowIds;
    }

    public long[] getColumnIds() {
        return columnIds;
    }

    public void setColumnIds(long[] columnIds) {
        this.columnIds = columnIds;
    }

    public String[] getRowNames() {
        return rowNames;
    }

    public void setRowNames(String[] rowNames) {
        this.rowNames = rowNames;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public double[][] getValues() {
        return values;
    }

    public void setValues(double[][] values) {
        this.values = values;
    }
}
