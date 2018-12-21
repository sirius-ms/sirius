package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.sirius.ExperimentResult;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public interface ProjectSpace extends ProjectWriter, ProjectReader {


    default void registerSummaryWriter(SummaryWriter... writers) {
        registerSummaryWriter(Arrays.asList(writers));
    }

    void registerSummaryWriter(List<SummaryWriter> writerList);

    default boolean removeSummaryWriter(SummaryWriter... writers) {
        return removeSummaryWriter(Arrays.asList(writers));
    }

    boolean removeSummaryWriter(List<SummaryWriter> writerList);

    void writeSummaries();

    int getNumberOfWrittenExperiments();

    default void writeExperiments() {
        forEach(expDir -> {
            try {
                writeExperiment(parseExperiment(expDir));
            } catch (IOException e) {
                LoggerFactory.getLogger(ProjectSpace.class).error("Could not Write experiment " + expDir.getDirectoryName(), e);
            }
        });
    }


    Iterable<ExperimentResult> parseExperiments();
}
