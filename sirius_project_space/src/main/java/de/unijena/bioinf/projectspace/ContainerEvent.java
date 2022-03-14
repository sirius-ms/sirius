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
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ContainerEvent<ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>> {

    public enum EventType {
        ID_CREATED, CREATED, UPDATED, DELETED, ID_FLAG
    }

    protected final EventType type;
    protected final List<ID> ids;
    protected final Map<ID,Container> containers;
    protected final Set<Class<? extends DataAnnotation>> affectedComponents;
    protected final EnumSet<CompoundContainerId.Flag> affectedIdFlags;

    public ContainerEvent(EventType type, ID id, Set<Class<? extends DataAnnotation>> affectedComponents) {
        this(type, id, affectedComponents, EnumSet.noneOf(CompoundContainerId.Flag.class));
    }
    public ContainerEvent(EventType type, ID id, Set<Class<? extends DataAnnotation>> affectedComponents, EnumSet<CompoundContainerId.Flag> affectedIdFlags) {
        this(type, List.of(id), Map.of(), affectedComponents, affectedIdFlags);
    }

    public ContainerEvent(@NotNull EventType type, @NotNull Container container, Set<Class<? extends DataAnnotation>> affectedComponents) {
        this(type, container, affectedComponents, EnumSet.noneOf(CompoundContainerId.Flag.class));
    }

    public ContainerEvent(@NotNull EventType type, @NotNull Container container, Set<Class<? extends DataAnnotation>> affectedComponents, EnumSet<CompoundContainerId.Flag> affectedIdFlags) {
        this(type, List.of(container.getId()), Map.of(container.getId(),container), affectedComponents, affectedIdFlags);
    }

    public ContainerEvent(@NotNull EventType type, @NotNull List<ID> ids, Set<Class<? extends DataAnnotation>> affectedComponents, EnumSet<CompoundContainerId.Flag> affectedIdFlags) {
        this(type, ids, Map.of(), affectedComponents, affectedIdFlags);
    }
    public ContainerEvent(@NotNull List<Container> containers, @NotNull EventType type, Set<Class<? extends DataAnnotation>> affectedComponents, EnumSet<CompoundContainerId.Flag> affectedIdFlags) {
        this(type, containers.stream().map(Container::getId).collect(Collectors.toList()),
                containers.stream().collect(Collectors.toMap(ProjectSpaceContainer::getId, c -> c)),
                affectedComponents, affectedIdFlags);
    }

    private ContainerEvent(EventType type, List<ID> ids, Map<ID,Container> containers, Set<Class<? extends DataAnnotation>> affectedComponents, EnumSet<CompoundContainerId.Flag> affectedIdFlags) {
        this.type = type;
        this.ids = ids;
        this.containers = containers;
        this.affectedComponents = affectedComponents;
        this.affectedIdFlags = affectedIdFlags;
    }

    public List<ID> getAffectedIDs() {
        return Collections.unmodifiableList(ids);
    }
    public ID getAffectedID() {
        return ids.get(0);
    }

    public boolean hasAffectedID() {
        return ids.size()>0;
    }

    public Set<Class<? extends DataAnnotation>> getAffectedComponents() {
        return affectedComponents;
    }

    public EnumSet<CompoundContainerId.Flag> getAffectedIdFlags() {
        return affectedIdFlags;
    }

    public boolean hasChanged(Class<? extends DataAnnotation> komponent) {
        return affectedComponents.contains(komponent);
    }

    public boolean hasChanged(CompoundContainerId.Flag flag) {
        return affectedIdFlags.contains(flag);
    }

    public boolean isFlagChange() {
        return type == EventType.ID_FLAG;
    }
    public boolean isUpdate() {
        return type == EventType.UPDATED;
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
    public <T extends DataAnnotation> Optional<T> getAffectedComponent(@NotNull Class<T> komponent) {
        return getAffectedComponent(ids.get(0),komponent);
    }

    public <T extends DataAnnotation> Optional<T> getAffectedComponent(@NotNull ID id, @NotNull Class<T> komponent) {
        if (containers == null || containers.isEmpty() || containers.containsKey(id))
            return Optional.empty();
        return containers.get(id).getAnnotation(komponent);
    }




}
