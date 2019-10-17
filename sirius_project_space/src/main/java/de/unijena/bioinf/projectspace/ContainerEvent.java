package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.annotations.DataAnnotation;

import java.util.Optional;
import java.util.Set;

public class ContainerEvent<ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>> {

    public enum EventType {
        CREATED, UPDATED, DELETED;
    }

    protected final EventType type;
    protected final ID id;
    protected final Container container;
    protected final Set<Class<? extends DataAnnotation>> affectedComponents;

    public ContainerEvent(EventType type, ID id, Container container, Set<Class<? extends DataAnnotation>> affectedComponents) {
        this.type = type;
        this.id = id;
        this.container = container;
        this.affectedComponents = affectedComponents;
    }

    public ID getAffectedID() {
        return id;
    }

    public Set<Class<? extends DataAnnotation>> getAffectedComponents() {
        return affectedComponents;
    }

    public boolean hasChanged(Class<? extends DataAnnotation> komponent) {
        return affectedComponents.contains(komponent);
    }

    public boolean isUpdate() {
        return type==EventType.UPDATED;
    }

    public boolean isCreated() {
        return type==EventType.CREATED;
    }

    public boolean isDeleted() {
        return type==EventType.DELETED;
    }

    public boolean isComponentDeleted(Class<? extends DataAnnotation> komponent) {
        return hasChanged(komponent) && getAffectedComponent(komponent).isEmpty();
    }

    /**
     * we do not want to give full access to the container, because writing into an updating container would result
     * into a self-loop.
     * Instead, it is possible to read annotations from the container, IFF they are part of the update.
     * @param komponent
     * @param <T>
     * @return
     */
    public <T extends DataAnnotation> Optional<T> getAffectedComponent(Class<T> komponent) {
        return container.getAnnotation(komponent);
    }




}
