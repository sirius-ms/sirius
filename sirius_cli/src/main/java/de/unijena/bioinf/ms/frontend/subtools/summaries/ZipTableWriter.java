package de.unijena.bioinf.ms.frontend.subtools.summaries;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipTableWriter implements SummaryTableWriter {

    private final ZipOutputStream zipOutputStream;
    private final TsvTableWriter tsvWriter;

    public ZipTableWriter(Path location, String filenameWithoutExtension, boolean quoteStrings) throws IOException {
        zipOutputStream = new ZipOutputStream(Files.newOutputStream(location.resolve(filenameWithoutExtension + ".tsv.zip")));
        ZipEntry entry = new ZipEntry(filenameWithoutExtension + ".tsv");
        zipOutputStream.putNextEntry(entry);
        tsvWriter = new TsvTableWriter(new BufferedWriter(new OutputStreamWriter(zipOutputStream)), quoteStrings);
    }


    @Override
    public void writeHeader(List<String> columns) throws IOException {
        tsvWriter.writeHeader(columns);
    }

    @Override
    public void writeRow(List<Object> row) throws IOException {
        tsvWriter.writeRow(row);
    }

    @Override
    public void flush() throws IOException {
        tsvWriter.flush();
        zipOutputStream.closeEntry();
    }

    @Override
    public void close() throws Exception {
        if (zipOutputStream != null) {
            zipOutputStream.close();
        }
        if (tsvWriter != null) {
            tsvWriter.close();
        }
    }
}
