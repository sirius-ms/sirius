package de.unijena.bioinf.ms.frontend.subtools.summaries;

import lombok.Setter;

import java.io.IOException;
import java.util.List;

public abstract class PrefixingSummaryWriter implements SummaryTableWriter {

    public static final String SIRIUS_COLUMN_PREFIX = "SIRIUS_";

    @Setter
    protected boolean siriusPrefix;

    protected List<String> addPrefix(List<String> columns) {
        return columns.stream().map(c -> SIRIUS_COLUMN_PREFIX + c).toList();
    }

    @Override
    public void writeHeader(List<String> columns) throws IOException {
        if (siriusPrefix) {
            columns = addPrefix(columns);
        }
        writeColumnNames(columns);
    }

    protected abstract void writeColumnNames(List<String> columns) throws IOException ;
}
