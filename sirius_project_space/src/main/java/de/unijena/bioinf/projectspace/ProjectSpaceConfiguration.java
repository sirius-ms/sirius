package de.unijena.bioinf.projectspace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProjectSpaceConfiguration {

    private final HashMap<Class, ComponentSerializer> componentSerializers;
    private final HashMap<Class, ContainerSerializer> containerSerializers;
    private final HashMap<Class, List<Class>> containerComponents;

    public ProjectSpaceConfiguration() {
        this.componentSerializers = new HashMap<>();
        this.containerSerializers = new HashMap<>();
        this.containerComponents = new HashMap<>();
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
    ContainerSerializer<ID,Container> getContainerSerializer(Class<Container> klass) {
        return (ContainerSerializer<ID,Container>)containerSerializers.get(klass);
    }

    @SuppressWarnings("unchecked")
    public <ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>, Component>
    ComponentSerializer<ID,Container,Component> getComponentSerializer(Class<Container> klass, Class<Component> componentClass) {
        return (ComponentSerializer<ID,Container,Component>)componentSerializers.get(componentClass);
    }

    public <T extends ProjectSpaceContainer<?>>
    Iterable<Class> getAllComponentsForContainer(Class<T> containerKlass) {
        return containerComponents.get(containerKlass);
    }
}
