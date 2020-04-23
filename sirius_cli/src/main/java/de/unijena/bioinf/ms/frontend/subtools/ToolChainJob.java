package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.jjobs.JobSubmitter;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.ms.annotations.RecomputeResults;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ToolChainJob<T> extends ProgressJJob<T> {

    default boolean isRecompute(final @NotNull Instance inst) {
        return inst.getExperiment().getAnnotation(RecomputeResults.class, () -> RecomputeResults.FALSE).value;
    }

    default boolean enableRecompute(final @NotNull Instance inst) {
        return setRecompute(inst, true);
    }

    default boolean disableRecompute(final @NotNull Instance inst) {
        return setRecompute(inst, false);
    }

    default boolean setRecompute(final @NotNull Instance inst, boolean recompute) {
        return inst.getExperiment().setAnnotation(RecomputeResults.class, RecomputeResults.newInstance(recompute));
    }

    boolean isAlreadyComputed(final @NotNull Instance inst);

    default String getToolName() {
        return getClass().getSimpleName();
    }

    void setInvalidator(Consumer<Instance> invalidator);

    void invalidateResults(final @NotNull Instance inst);


    interface Factory<J extends ToolChainJob<?>> {
        J makeJob(JobSubmitter submitter);

        Factory<J> addInvalidator(Consumer<Instance> invalidator);
    }

    abstract class FactoryImpl<J extends ToolChainJob<?>> implements Factory<J> {
        @NotNull
        protected Function<JobSubmitter, J> jobCreator;
        @NotNull
        protected Consumer<Instance> invalidator;

        public FactoryImpl(@NotNull Function<JobSubmitter, J> jobCreator, @Nullable Consumer<Instance> baseInvalidator) {
            this.jobCreator = jobCreator;
            invalidator = baseInvalidator != null ? baseInvalidator : (instance -> {});
        }

        @Override
        public J makeJob(JobSubmitter submitter) {
            J j = jobCreator.apply(submitter);
            j.setInvalidator(invalidator);
            return j;
        }

        @Override
        public Factory<J> addInvalidator(Consumer<Instance> invalidator) {
            this.invalidator = this.invalidator.andThen(invalidator);
            return this;
        }
    }
}
