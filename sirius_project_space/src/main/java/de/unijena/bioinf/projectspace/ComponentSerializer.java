package de.unijena.bioinf.projectspace;

public interface ComponentSerializer<A extends ProjectSpaceContainerId, B extends ProjectSpaceContainer<A>, C> {

    public C read(ProjectReader reader, A id, B container);

    public void write(ProjectWriter writer, A id, B container, C component);

    public void delete(ProjectWriter writer, A id, B container, C component);

}
