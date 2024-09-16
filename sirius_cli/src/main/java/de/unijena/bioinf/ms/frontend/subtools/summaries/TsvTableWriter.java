package de.unijena.bioinf.ms.frontend.subtools.summaries;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TsvTableWriter extends DelimitedTableWriter {

    private final static String DELIMITER = "\t";

    public TsvTableWriter(Path location, String filenameWithoutExtension, boolean quoteStrings) throws IOException {
        super(Files.newBufferedWriter(location.resolve(filenameWithoutExtension + ".tsv")), DELIMITER, quoteStrings);
    }

    public TsvTableWriter(BufferedWriter writer, boolean quoteStrings) {
        super(writer, DELIMITER, quoteStrings);
    }
}
