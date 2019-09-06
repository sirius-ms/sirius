package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ms.annotaions.RecomputeResults;
import org.jetbrains.annotations.NotNull;

public interface SubToolJob {
    default void invalidateResults(final @NotNull Instance result) {
        final RecomputeResults recomp = result.getExperiment().getAnnotation(RecomputeResults.class);
        if (recomp == null || !recomp.value)
            result.getExperiment().setAnnotation(RecomputeResults.class, RecomputeResults.TRUE);
    }

    default boolean isRecompute(final @NotNull Instance expRes) {
        return expRes.getExperiment().getAnnotation(RecomputeResults.class, () -> RecomputeResults.FALSE).value;
    }

}
