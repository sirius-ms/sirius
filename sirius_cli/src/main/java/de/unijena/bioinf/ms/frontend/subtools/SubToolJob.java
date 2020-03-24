package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ms.annotations.RecomputeResults;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface SubToolJob {
    default void invalidateResults(final @NotNull Instance result) {
        final Optional<RecomputeResults> recomp = result.getExperiment().getAnnotation(RecomputeResults.class);
        if (recomp.isEmpty() || !recomp.get().value)
            result.getExperiment().setAnnotation(RecomputeResults.class, RecomputeResults.TRUE);
    }

    default boolean isRecompute(final @NotNull Instance expRes) {
        return expRes.getExperiment().getAnnotation(RecomputeResults.class, () -> RecomputeResults.FALSE).value;
    }

}
