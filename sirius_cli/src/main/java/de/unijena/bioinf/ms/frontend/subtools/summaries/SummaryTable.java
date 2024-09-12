package de.unijena.bioinf.ms.frontend.subtools.summaries;

import de.unijena.bioinf.ms.persistence.model.core.DataSource;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;

import java.io.IOException;
import java.nio.file.Path;

public abstract class SummaryTable implements AutoCloseable {

    protected final SummaryTableWriter writer;

    protected SummaryTable(SummaryTableWriter writer) {
        this.writer = writer;
    }

    public void flush() throws IOException {
        writer.flush();
    }

    public static String getMappingIdOrFallback(AlignedFeatures feature){
        String id = feature.getExternalFeatureId();
        if (id != null && !id.isBlank())
            return id;

        StringBuilder builder = new StringBuilder(String.valueOf(feature.getAlignedFeatureId()));

        DataSource source = feature.getDataSource().orElse(DataSource.builder().format(DataSource.Format.UNKNOWN).build());
        if (source.getFormat() == DataSource.Format.UNKNOWN || source.getSource() == null || source.getSource().isBlank()) {
            builder.append("_").append(DataSource.Format.UNKNOWN.name());
        } else {
            String fileName = Path.of(source.getSource()).getFileName().toString();
            builder.append("_").append(fileName, 0, fileName.lastIndexOf('.'));
        }
        String name = feature.getName();
        if (name != null && !name.isBlank())
            builder.append("_").append(name);

        return builder.toString();
    }

    @Override
    public void close() throws Exception {
        writer.close();
    }
}
