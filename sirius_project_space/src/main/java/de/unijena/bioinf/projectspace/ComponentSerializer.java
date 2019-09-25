package de.unijena.bioinf.projectspace;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;

public interface ComponentSerializer<A extends ProjectSpaceContainerId, B extends ProjectSpaceContainer<A>, C> {

    @Nullable
    public C read(ProjectReader reader, A id, B container)  throws IOException;

    public void write(ProjectWriter writer, A id, B container, Optional<C> component)  throws IOException;

    public void delete(ProjectWriter writer, A id)  throws IOException;

}
