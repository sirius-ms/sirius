package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ms.annotations.DataAnnotation;

import java.util.EnumSet;
import java.util.EventListener;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface ContainerListener<ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>> extends EventListener {

    public void containerChanged(ContainerEvent<ID, Container> event);

    static class PartiallyListeningFluentBuilder<ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>> {
        protected final ConcurrentLinkedQueue<ContainerListener<ID,Container>> queue;
        protected Class[] classes;
        protected EnumSet<ContainerEvent.EventType> types;

        protected PartiallyListeningFluentBuilder(ConcurrentLinkedQueue queue) {
            classes = new Class[0];
            this.queue = queue;
            types = null;
        }

        protected PartiallyListeningFluentBuilder(ConcurrentLinkedQueue queue, Class[] classes, EnumSet<ContainerEvent.EventType> types) {
            this.classes = classes;
            this.types = types;
            this.queue = queue;
        }

        public PartiallyListeningFluentBuilder<ID,Container> on(ContainerEvent.EventType type) {
            EnumSet<ContainerEvent.EventType> types = this.types==null ? EnumSet.noneOf(ContainerEvent.EventType.class) : this.types;
            types.add(type);
            return new PartiallyListeningFluentBuilder(queue, classes, types);
        }

        public PartiallyListeningFluentBuilder<ID,Container> onCreate() {
            return on(ContainerEvent.EventType.CREATED);
        }

        public PartiallyListeningFluentBuilder<ID,Container> onUpdate() {
            return on(ContainerEvent.EventType.UPDATED);
        }

        public PartiallyListeningFluentBuilder<ID,Container> onDelete() {
            return on(ContainerEvent.EventType.DELETED);
        }

        public PartiallyListeningFluentBuilder onlyFor(Class<? extends DataAnnotation>... klasses) {
            return new PartiallyListeningFluentBuilder(queue, klasses, types);
        }

        public Defined thenDo(ContainerListener<ID,Container> listener) {
            EnumSet<ContainerEvent.EventType> types = this.types==null ? EnumSet.allOf(ContainerEvent.EventType.class) : this.types;
            return new Defined(queue, new PartiallyListeningUpdateListener<ID,Container>(classes, types, listener));
        }

    }

    public static class Defined {
        private final PartiallyListeningUpdateListener listener;
        private final ConcurrentLinkedQueue queue;
        protected boolean registered;

        protected Defined(ConcurrentLinkedQueue queue, PartiallyListeningUpdateListener listener) {
            this.listener = listener;
            this.queue = queue;
            this.registered = false;
        }

        public Defined register() {
            if (!registered) {
                registered = true;
                queue.add(listener);
            }
            return this;
        }

        public Defined unregister() {
            if (registered) {
                queue.remove(listener);
                registered = false;
            }
            return this;
        }
    }

    /**
     * A container listener, which is only called when a specific component is updated.
     */
    public static class PartiallyListeningUpdateListener<ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>> implements ContainerListener<ID,Container> {

        protected final Class[] listeningClasses;
        protected final EnumSet<ContainerEvent.EventType> listenToTypes;

        protected final ContainerListener<ID,Container> listener;

        PartiallyListeningUpdateListener(Class[] listeningClasses, EnumSet<ContainerEvent.EventType> listenToTypes,  ContainerListener<ID,Container> listener) {
            this.listeningClasses = listeningClasses;
            this.listenToTypes = listenToTypes;
            this.listener = listener;
        }

        @Override
        public void containerChanged(ContainerEvent<ID,Container> event) {
            if (listenToTypes.contains(event.type)) {
                if (listeningClasses.length==0) listener.containerChanged(event);
                else {
                    for (Class k : listeningClasses) {
                        if (event.hasChanged(k)) {
                            listener.containerChanged(event);
                            return;
                        }
                    }
                }
            }
        }
    }

}
