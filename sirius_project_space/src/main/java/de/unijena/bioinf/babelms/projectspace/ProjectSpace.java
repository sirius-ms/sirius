package de.unijena.bioinf.babelms.projectspace;

import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
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

    void writeSummaries(Iterable<ExperimentResult> resultsToSummarize);

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


    @NotNull
    default Iterable<ExperimentResult> parseExperiments() {
        return this::parseExperimentIterator;
    }

    @NotNull
    Iterator<ExperimentResult> parseExperimentIterator();
}
