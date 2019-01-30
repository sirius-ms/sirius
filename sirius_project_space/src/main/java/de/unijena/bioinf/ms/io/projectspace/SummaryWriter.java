package de.unijena.bioinf.ms.io.projectspace;

import de.unijena.bioinf.sirius.ExperimentResult;

import java.util.Collections;
import java.util.Map;

@FunctionalInterface
public interface SummaryWriter {
    void writeSummary(Iterable<ExperimentResult> experiments, DirectoryWriter writer);

    default Map<String, String> getVersionInfo() {
        return Collections.emptyMap();
    }
}
