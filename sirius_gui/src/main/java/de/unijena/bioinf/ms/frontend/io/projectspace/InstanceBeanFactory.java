package de.unijena.bioinf.ms.frontend.io.projectspace;

import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import org.jetbrains.annotations.NotNull;

public class InstanceBeanFactory implements InstanceFactory<InstanceBean> {
    @Override
    public InstanceBean create(@NotNull CompoundContainer compoundContainer, @NotNull ProjectSpaceManager spaceManager) {
        return new InstanceBean(compoundContainer,spaceManager);
    }
}
