package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.sirius.ExperimentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public interface ProjectSpace extends ProjectWriter, Iterable<ExperimentResult> {
    Logger LOG = LoggerFactory.getLogger(ProjectSpace.class);

    void deleteExperiment(ExperimentDirectory id) throws IOException;

    ExperimentResult loadExperiment(ExperimentDirectory id) throws IOException;

    default void registerSummaryWriter(SummaryWriter... writers) {
        registerSummaryWriter(Arrays.asList(writers));
    }

    void registerSummaryWriter(List<SummaryWriter> writerList);

    void writeSummaries();

    default void close() {
        writeSummaries();
    }



    /* class ID {
     *//*
     * This is the makePath (relative to workspace root)
     * were the Entity corresponding to this Index is serialized
     *//*
        private String filePath;

        protected ID(@NotNull String filePath) {
            this.filePath = filePath;
        }

        public @NotNull String getFilePath() {
            return filePath;
        }

        protected void setFilePath(@NotNull String filePath) {
            this.filePath = filePath;
        }
    }*/
}
