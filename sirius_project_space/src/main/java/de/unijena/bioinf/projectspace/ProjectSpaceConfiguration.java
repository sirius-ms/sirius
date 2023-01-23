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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ProjectSpaceConfiguration {
    private final HashMap<Class, ComponentSerializer> componentSerializers;
    private final HashMap<Class, ContainerSerializer> containerSerializers;
    private final HashMap<Class, List<Class>> containerComponents;

    private final HashMap<Class, ComponentSerializer> projectSpacePropertySerializers;

    public ProjectSpaceConfiguration() {
        this.componentSerializers = new HashMap<>();
        this.containerSerializers = new HashMap<>();
        this.containerComponents = new HashMap<>();
        this.projectSpacePropertySerializers = new HashMap<>();
    }

    public <T extends ProjectSpaceProperty> void defineProjectSpaceProperty(Class<T> propertyClass, ComponentSerializer<ProjectSpaceContainerId,ProjectSpaceContainer<ProjectSpaceContainerId>, T> serializer) {
        this.projectSpacePropertySerializers.put(propertyClass, serializer);
    }

    public <T extends ProjectSpaceProperty> ComponentSerializer<ProjectSpaceContainerId,ProjectSpaceContainer<ProjectSpaceContainerId>, T>  getProjectSpacePropertySerializer(Class<T> propertyClass) {
        return projectSpacePropertySerializers.get(propertyClass);
    }

    public <T extends ProjectSpaceProperty> boolean hasProjectSpacePropertyRegistered(Class<T> propertyClass){
        return projectSpacePropertySerializers.containsKey(propertyClass);
    }

    public <ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>>
    void registerContainer(Class<Container> container, ContainerSerializer<ID, Container> serializer) {
        containerSerializers.put(container, serializer);
    }

    public <ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>, Component>
    void registerComponent(Class<Container> container, Class<Component> componentClass, ComponentSerializer<ID, Container, Component> serializer) {
        componentSerializers.put(componentClass, serializer);
        containerComponents.computeIfAbsent(container, (n)->new ArrayList<>()).add(componentClass);
    }

    @SuppressWarnings("unchecked")
    public <ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>>
    @NotNull  ContainerSerializer<ID,Container> getContainerSerializer(Class<Container> klass) {
        final ContainerSerializer containerSerializer = containerSerializers.get(klass);
        if (containerSerializer==null)
            throw new IllegalArgumentException("No container of class '" + klass.getSimpleName() + "' registered");
        return (ContainerSerializer<ID,Container>) containerSerializer;
    }

    @SuppressWarnings("unchecked")
    public <ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>, Component>
    @NotNull ComponentSerializer<ID,Container,Component> getComponentSerializer(Class<Container> klass, Class<Component> componentClass) {
        final ComponentSerializer componentSerializer = componentSerializers.get(componentClass);
        if (componentSerializer==null)
            throw new IllegalArgumentException("No component of class '" + componentClass.getSimpleName() + "' registered.");
        return (ComponentSerializer<ID,Container,Component>) componentSerializer;
    }

    public <T extends ProjectSpaceContainer<?>>
    List<Class> getAllComponentsForContainer(Class<T> containerKlass) {
        return Collections.unmodifiableList(containerComponents.get(containerKlass));
    }
}
