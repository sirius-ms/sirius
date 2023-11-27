/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.events;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvent;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvents;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class SseEventService implements EventService<SseEmitter> {

    private final Map<ServerEvent.Type, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final BlockingQueue<ServerEvent<?>> events = new LinkedBlockingQueue<>();
    private EventRunner eventRunner = null;

    public SseEmitter createEventSender(@NotNull EnumSet<ServerEvent.Type> typesToListenOn) {
        final SseEmitter emitter = new SseEmitter();
        addEventSender(emitter, typesToListenOn);
        return emitter;
    }

    public void addEventSender(@NotNull SseEmitter emitter, @NotNull EnumSet<ServerEvent.Type> typesToListenOn) {
        //configure shutdown
        emitter.onCompletion(() -> emitters.values().forEach(v -> v.remove(emitter)));
        emitter.onTimeout(() -> emitters.values().forEach(v -> v.remove(emitter)));
        //add
        typesToListenOn.forEach(type -> emitters.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>()).add(emitter));
    }

    public void sendEvent(ServerEvent<?> event) { //todo maybe we need some wait here to not spam events to fast...
        if (eventRunner == null)
            eventRunner = (EventRunner) SiriusJobs.runInBackground(new EventRunner());
        events.add(event);
    }

    @Override
    public void shutdown() {
//        events.clear(); //todo do we want to clear event bevor shutdown for faster but less clean shutdown?
        eventRunner.shutdown();
        emitters.values().stream().flatMap(Collection::stream).distinct().forEach(ResponseBodyEmitter::complete);
    }

    private class EventRunner extends TinyBackgroundJJob<Boolean> {
        @Override
        protected Boolean compute() {
            try {
                ServerEvent<?> eventData = null;
                while (eventData != ServerEvents.EMPTY_EVENT()) {
                    checkForInterruption();
                    try {
                        eventData = events.take();

                        if (eventData == ServerEvents.EMPTY_EVENT())
                            return true;

                        for (SseEmitter emitter : emitters.getOrDefault(eventData.getEventType(), List.of())) {
                            try {
                                emitter.send(SseEmitter.event()
                                        .data(eventData, MediaType.APPLICATION_JSON)
                                        .name(eventData.getEventType().name()));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                                emitters.values().forEach(v -> v.remove(emitter));
                            }
                        }
                    } catch (InterruptedException e) {
                        if (eventData == ServerEvents.EMPTY_EVENT())
                            return true;
                        checkForInterruption();
                    }
                }
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        }

        public synchronized void shutdown() {
            events.add(ServerEvents.EMPTY_EVENT());
        }
    }
}