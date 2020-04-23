package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.ms.annotations.RecomputeResults;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface ToolChainJob<T> extends ProgressJJob<T> {
    default void invalidateResults(final @NotNull Instance inst) {
        final Optional<RecomputeResults> recomp = inst.getExperiment().getAnnotation(RecomputeResults.class);
        if (recomp.isEmpty() || !recomp.get().value)
            inst.getExperiment().setAnnotation(RecomputeResults.class, RecomputeResults.TRUE);

        getInvalidator().invalidate(inst);
    }

    default boolean isRecompute(final @NotNull Instance expRes) {
        return expRes.getExperiment().getAnnotation(RecomputeResults.class, () -> RecomputeResults.FALSE).value;
    }

    default String getToolName() {
        return getClass().getSimpleName();
    }

    Invalidator getInvalidator();
    void setInvalidator(Invalidator invalidator);

    @FunctionalInterface
    interface Invalidator {
        void invalidate(Instance inst);
    }

    @FunctionalInterface
    interface Factory<J extends ToolChainJob<?>> {
        J makeJob(JobSubmitter submitter);
    }
}
