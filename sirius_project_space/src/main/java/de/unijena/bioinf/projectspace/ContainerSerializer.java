package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.projectspace.sirius.CompoundContainer;

public interface ContainerSerializer<S extends ProjectSpaceContainerId, T extends ProjectSpaceContainer<S>> {

    public void writeToProjectSpace(ProjectWriter.ForContainer writer, S id, T container);
    public T readFromProjectSpace(ProjectReader.ForContainer reader, S id);
    public void deleteFromProjectSpace(ProjectWriter.ForContainer writer, S id, T container);

}
