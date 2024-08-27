package de.unijena.bioinf.ms.frontend.subtools.summaries;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class TsvTableWriter implements SummaryTableWriter {

    private final static String SEPARATOR = "\t";
    private final static String DOUBLE_FORMAT = "%.3f";
    private final static String LONG_FORMAT = "%d";

    private final BufferedWriter w;

    @Setter
    private String stringFormat = "%s";

    public TsvTableWriter(Path location, String filenameWithoutExtension) throws IOException {
        this.w = Files.newBufferedWriter(location.resolve(filenameWithoutExtension + ".tsv"));
    }

    public TsvTableWriter(BufferedWriter writer) {
        this.w = writer;
    }

    @Override
    public void writeHeader(List<String> columns) throws IOException {
        w.write(String.join(SEPARATOR, columns));
        w.newLine();
    }

    @Override
    public void writeRow(List<Object> row) throws IOException {
        w.write(row.stream().map(this::getString).collect(Collectors.joining(SEPARATOR)));
        w.newLine();
    }

    private String getString(Object val) {
        if (val == null || val.equals(Double.NaN)) return "";
        if (val instanceof String) return String.format(stringFormat, val);
        if (val instanceof Double) return String.format(DOUBLE_FORMAT, val);
        if (val instanceof Integer) return String.format(LONG_FORMAT, val);
        if (val instanceof Long) return String.format(LONG_FORMAT, val);
        log.warn("TSV writer encountered a value of an unexpected type {} {}", val, val.getClass());
        return String.valueOf(val);
    }

    @Override
    public void flush() throws IOException {
        w.flush();
    }

    @Override
    public void close() throws IOException {
        if (w != null) {
            w.close();
        }
    }
}
