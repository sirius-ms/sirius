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
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.EventListener;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface ContainerListener<ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>> extends EventListener {

    void containerChanged(ContainerEvent<ID, Container> event);

    class PartiallyListeningFluentBuilder<ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>> {
        protected final ConcurrentLinkedQueue<ContainerListener<ID, Container>> queue;
        protected ID idToListenOn;
        protected Class[] classes;
        protected EnumSet<ContainerEvent.EventType> types;

        protected PartiallyListeningFluentBuilder(ConcurrentLinkedQueue<ContainerListener<ID, Container>> queue) {
            this(queue, new Class[0], null);
        }

        protected PartiallyListeningFluentBuilder(ConcurrentLinkedQueue<ContainerListener<ID, Container>> queue, Class[] classes, EnumSet<ContainerEvent.EventType> types) {
            this(null, queue, classes, types);
        }

        protected PartiallyListeningFluentBuilder(@Nullable ID idToListenOn, ConcurrentLinkedQueue<ContainerListener<ID, Container>> queue, Class[] classes, EnumSet<ContainerEvent.EventType> types) {
            this.idToListenOn = idToListenOn;
            this.classes = classes;
            this.types = types;
            this.queue = queue;
        }



        public PartiallyListeningFluentBuilder<ID, Container> on(ContainerEvent.EventType type) {
            EnumSet<ContainerEvent.EventType> types = this.types == null ? EnumSet.noneOf(ContainerEvent.EventType.class) : this.types;
            types.add(type);
            return new PartiallyListeningFluentBuilder<>(queue, classes, types);
        }

        public PartiallyListeningFluentBuilder<ID, Container> onCreate() {
            return on(ContainerEvent.EventType.CREATED);
        }

        public PartiallyListeningFluentBuilder<ID,Container> onUpdate() {
            return on(ContainerEvent.EventType.UPDATED);
        }

        public PartiallyListeningFluentBuilder<ID,Container> onDelete() {
            return on(ContainerEvent.EventType.DELETED);
        }

        public PartiallyListeningFluentBuilder<ID, Container> onlyFor(ID idToListenOn) {
            return new PartiallyListeningFluentBuilder<>(idToListenOn, queue, classes, types);
        }

        public PartiallyListeningFluentBuilder<ID,Container> onlyFor(Class<? extends DataAnnotation>... klasses) {
            return new PartiallyListeningFluentBuilder<>(queue, klasses, types);
        }

        public Defined thenDo(ContainerListener<ID,Container> listener) {
            EnumSet<ContainerEvent.EventType> types = this.types==null ? EnumSet.allOf(ContainerEvent.EventType.class) : this.types;
            return new Defined(queue, new PartiallyListeningUpdateListener<>(idToListenOn, classes, types, listener));
        }

    }

    class Defined {
        private final PartiallyListeningUpdateListener listener;
        private final ConcurrentLinkedQueue queue;
        protected boolean registered;

        protected <ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>> Defined(ConcurrentLinkedQueue<ContainerListener<ID,Container>> queue, PartiallyListeningUpdateListener<ID,Container> listener) {
            this.listener = listener;
            this.queue = queue;
            this.registered = false;
        }

        public boolean isRegistered() {
            return registered;
        }

        public boolean notRegistered() {
            return !isRegistered();
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
    class PartiallyListeningUpdateListener<ID extends ProjectSpaceContainerId, Container extends ProjectSpaceContainer<ID>> implements ContainerListener<ID, Container> {

        protected final ID idToListenOn;
        protected final Class[] listeningClasses;
        protected final EnumSet<ContainerEvent.EventType> listenToTypes;
        protected final ContainerListener<ID, Container> listener;

        PartiallyListeningUpdateListener(@Nullable ID idToListenOn, @NotNull Class[] listeningClasses, @NotNull EnumSet<ContainerEvent.EventType> listenToTypes, @NotNull ContainerListener<ID, Container> listener) {
            this.idToListenOn = idToListenOn;
            this.listeningClasses = listeningClasses;
            this.listenToTypes = listenToTypes;
            this.listener = listener;
        }

        @Override
        public void containerChanged(ContainerEvent<ID, Container> event) {
            //todo should this always or never update, if no affected id is present (event type ID_FLAG)?
            if (!event.hasAffectedID() || (idToListenOn != null && !idToListenOn.equals(event.getAffectedID())))
                return;

            if (listenToTypes.contains(event.type)) {
                if (listeningClasses.length == 0)
                    listener.containerChanged(event);
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
