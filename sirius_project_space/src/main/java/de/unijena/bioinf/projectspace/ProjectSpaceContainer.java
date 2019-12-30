package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;

/**
 * A container stores several components which are serializable. It is identified via an ID.
 */
public abstract class ProjectSpaceContainer<S extends ProjectSpaceContainerId> implements Annotated<DataAnnotation> {
    public abstract S getId();
}
