package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.annotations.DataAnnotation;

/**
 * A container stores several components which are serializable. It is identified via an ID.
 */
public abstract class ProjectSpaceContainer<S extends ProjectSpaceContainerId> {

    protected abstract  <T extends DataAnnotation> T get(Class<T> klassname);
    protected abstract  <T extends DataAnnotation> void set(Class<T> klassname, T value);

    public abstract S getId();

}
