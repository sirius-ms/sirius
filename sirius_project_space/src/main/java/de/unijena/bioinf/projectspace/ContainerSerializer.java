package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.projectspace.sirius.CompoundContainer;

import java.io.IOException;

public interface ContainerSerializer<S extends ProjectSpaceContainerId, T extends ProjectSpaceContainer<S>> {

    public void writeToProjectSpace(ProjectWriter writer, ProjectWriter.ForContainer<S,T> containerSerializer, S id, T container)  throws IOException;
    public T readFromProjectSpace(ProjectReader reader, ProjectReader.ForContainer<S,T> containerSerializer, S id)  throws IOException;
    public void deleteFromProjectSpace(ProjectWriter writer, ProjectWriter.DeleteContainer<S> containerSerializer, S id)  throws IOException;

}
