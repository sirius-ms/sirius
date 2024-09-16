package de.unijena.bioinf.ms.frontend.subtools.summaries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CsvTableWriter extends DelimitedTableWriter {

    private final static String DELIMITER = ",";

    public CsvTableWriter(Path location, String filenameWithoutExtension, boolean quoteStrings) throws IOException {
        super(Files.newBufferedWriter(location.resolve(filenameWithoutExtension + ".csv")), DELIMITER, quoteStrings);
    }
}
