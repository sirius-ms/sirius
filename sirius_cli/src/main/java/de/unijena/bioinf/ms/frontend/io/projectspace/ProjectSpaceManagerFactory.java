package de.unijena.bioinf.ms.frontend.io.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

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
