package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.sirius.ExperimentResult;

@FunctionalInterface
public interface SummaryWriter {
    void writeSummary(Iterable<ExperimentResult> experiments, DirectoryWriter writer);
}
