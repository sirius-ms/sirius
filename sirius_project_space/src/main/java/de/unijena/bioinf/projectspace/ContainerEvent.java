/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.annotations.DataAnnotation;

import java.util.Optional;
import java.util.Set;

public class ContainerEvent<ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>> {

    public enum EventType {
        ID_CREATED, CREATED, UPDATED, DELETED;
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
        return type == EventType.UPDATED;
    }

    public boolean isUpdateRemove() {
        return type == EventType.UPDATED && container == null;
    }

    public boolean isCreated() {
        return type == EventType.CREATED;
    }

    public boolean isDeleted() {
        return type == EventType.DELETED;
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
        if (container == null)
            return Optional.empty();
        return container.getAnnotation(komponent);
    }




}
