package de.unijena.bioinf.ms.frontend.io.projectspace;

import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import org.jetbrains.annotations.NotNull;

interface InstanceFactory<I extends Instance> {
    I create(@NotNull CompoundContainer compoundContainer, @NotNull ProjectSpaceManager spaceManager);

    final class Default implements InstanceFactory<Instance> {

        @Override
        public Instance create(@NotNull CompoundContainer compoundContainer, @NotNull ProjectSpaceManager spaceManager) {
            return new Instance(compoundContainer, spaceManager);
        }
    }
}
