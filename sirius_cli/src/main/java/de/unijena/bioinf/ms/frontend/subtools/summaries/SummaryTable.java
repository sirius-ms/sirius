package de.unijena.bioinf.ms.frontend.subtools.summaries;

import java.io.IOException;

public abstract class SummaryTable implements AutoCloseable {

    protected final SummaryTableWriter writer;

    protected SummaryTable(SummaryTableWriter writer) {
        this.writer = writer;
    }

    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws Exception {
        writer.close();
    }
}
