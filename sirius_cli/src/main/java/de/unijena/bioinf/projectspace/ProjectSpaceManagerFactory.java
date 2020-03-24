package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface ProjectSpaceManagerFactory<M extends ProjectSpaceManager> {
    default M create(SiriusProjectSpace space) {
        return create(space, null);
    }


    default M create(@NotNull SiriusProjectSpace space, @Nullable Function<Ms2Experiment, String> formatter) {
        return create(space, new InstanceFactory.Default(), formatter);
    }

    M create(@NotNull SiriusProjectSpace space, @NotNull InstanceFactory<Instance> factory, @Nullable Function<Ms2Experiment, String> formatter);


    final class Default implements ProjectSpaceManagerFactory<ProjectSpaceManager> {
        @Override
        public ProjectSpaceManager create(@NotNull SiriusProjectSpace space, @NotNull InstanceFactory<Instance> factory, @Nullable Function<Ms2Experiment, String> formatter) {
            return new ProjectSpaceManager(space, factory, formatter);
        }
    }
}
