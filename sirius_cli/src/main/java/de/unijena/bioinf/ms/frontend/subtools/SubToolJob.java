package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ms.properties.RecomputeResults;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;

public interface SubToolJob {
    default void invalidateResults(final @NotNull ExperimentResult result) {
        final RecomputeResults recomp = result.getExperiment().getAnnotation(RecomputeResults.class);
        if (recomp == null || !recomp.value)
            result.getExperiment().setAnnotation(RecomputeResults.class, RecomputeResults.TRUE);
    }

    default boolean isRecompute(final @NotNull ExperimentResult expRes) {
        return expRes.getExperiment().getAnnotation(RecomputeResults.class, () -> RecomputeResults.FALSE).value;
    }

}
