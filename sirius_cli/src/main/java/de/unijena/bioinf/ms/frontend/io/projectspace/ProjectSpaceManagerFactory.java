package de.unijena.bioinf.ms.frontend.io.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

public interface ProjectSpaceManagerFactory<M extends ProjectSpaceManager> {
    default M create(SiriusProjectSpace space) {
        return create(space, null, null);
    }


    default M create(@NotNull SiriusProjectSpace space, @Nullable Function<Ms2Experiment, String> formatter, @Nullable Predicate<CompoundContainerId> compoundFilter) {
        return create(space, new InstanceFactory.Default(), formatter, compoundFilter);
    }

    M create(@NotNull SiriusProjectSpace space, @NotNull InstanceFactory<Instance> factory, @Nullable Function<Ms2Experiment, String> formatter, @Nullable Predicate<CompoundContainerId> compoundFilter);


    final class Default implements ProjectSpaceManagerFactory<ProjectSpaceManager> {
        @Override
        public ProjectSpaceManager create(@NotNull SiriusProjectSpace space, @NotNull InstanceFactory<Instance> factory, @Nullable Function<Ms2Experiment, String> formatter, @Nullable Predicate<CompoundContainerId> compoundFilter) {
            return new ProjectSpaceManager(space, factory, formatter, compoundFilter);
        }
    }
}
