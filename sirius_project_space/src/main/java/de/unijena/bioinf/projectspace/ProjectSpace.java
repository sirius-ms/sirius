package de.unijena.bioinf.projectspace;

import java.util.Optional;

public abstract class ProjectSpace {

    public abstract <S extends ProjectSpaceContainerId> Optional<S> find(String dirName);
    public abstract <S extends ProjectSpaceContainerId, T extends ProjectSpaceContainer<S>> T get(S id, Class<?>... components);
    public abstract <S extends ProjectSpaceContainerId, T extends ProjectSpaceContainer<S>> void update(T container, Class<?>... components);

}
