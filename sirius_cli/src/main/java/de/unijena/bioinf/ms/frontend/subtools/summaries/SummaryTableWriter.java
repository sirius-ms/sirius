package de.unijena.bioinf.ms.frontend.subtools.summaries;

import java.io.IOException;
import java.util.List;

public interface SummaryTableWriter extends AutoCloseable {

    /**
     * Column names and their corresponding types
     */
    void writeHeader(List<String> columns) throws IOException;

    /**
     * Null values will be empty cells, floating point values should be instances of Double, integers of Integer or Long
     */
    void writeRow(List<Object> row) throws IOException;

    /**
     * Finalize writing the summary file and flush the underlying stream
     */
    void flush() throws IOException;

}
