package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.annotations.ResultAnnotation;

import java.io.IOException;

public interface ComponentSerializer<A extends ProjectSpaceContainerId, B extends ProjectSpaceContainer<A>, C> {

    public C read(ProjectReader reader, A id, B container)  throws IOException;

    public void write(ProjectWriter writer, A id, B container, C component)  throws IOException;

    public void delete(ProjectWriter writer, A id)  throws IOException;

}
