package de.unijena.bioinf.ms.frontend.subtools.summaries;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
public class DelimitedTableWriter implements SummaryTableWriter {

    private final static Locale LOCALE = Locale.US;
    private final static String DOUBLE_FORMAT = "%.3f";
    private final static String LONG_FORMAT = "%d";
    private final static String STRING_QUOTED_FORMAT = "\"%s\"";

    private final BufferedWriter w;
    private final String delimiter;
    private final boolean quoteStrings;

    public DelimitedTableWriter(BufferedWriter writer, String delimiter, boolean quoteStrings) {
        this.w = writer;
        this.delimiter = delimiter;
        this.quoteStrings = quoteStrings;
    }

    @Override
    public void writeHeader(List<String> columns) throws IOException {
        w.write(columns.stream().map(this::formatString).collect(Collectors.joining(delimiter)));
        w.newLine();
    }

    private String formatString(String s) {
        if (quoteStrings || s.contains(delimiter)) {
            return String.format(LOCALE, STRING_QUOTED_FORMAT, s);
        }
        return s;
    }

    @Override
    public void writeRow(List<Object> row) throws IOException {
        w.write(row.stream().map(this::getString).collect(Collectors.joining(delimiter)));
        w.newLine();
    }

    private String getString(Object val) {
        if (val == null || val.equals(Double.NaN)) return "";
        if (val instanceof String || val instanceof Boolean || val instanceof Enum<?>) return formatString(val.toString());
        if (val instanceof Double) return String.format(LOCALE, DOUBLE_FORMAT, val);
        if (val instanceof Integer) return String.format(LOCALE, LONG_FORMAT, val);
        if (val instanceof Long) return String.format(LOCALE, LONG_FORMAT, val);
        log.warn("Delimited writer encountered a value of an unexpected type {} {}", val, val.getClass());
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
