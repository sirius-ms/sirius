package de.unijena.bioinf.ms.projectspace;

import java.util.Arrays;
import java.util.List;

public interface ProjectSpace extends ProjectWriter, ProjectReader {


//    ExperimentResult loadExperiment(ExperimentDirectory id) throws IOException;

    default void registerSummaryWriter(SummaryWriter... writers) {
        registerSummaryWriter(Arrays.asList(writers));
    }

    void registerSummaryWriter(List<SummaryWriter> writerList);

    default boolean removeSummaryWriter(SummaryWriter... writers) {
        return removeSummaryWriter(Arrays.asList(writers));
    }

    boolean removeSummaryWriter(List<SummaryWriter> writerList);

    void writeSummaries();

    default void close() {
        writeSummaries();
    }
}
