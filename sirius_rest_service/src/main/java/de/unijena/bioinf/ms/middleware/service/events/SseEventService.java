/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.events;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.middleware.model.compute.Job;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvent;
import de.unijena.bioinf.ms.middleware.model.events.ServerEvents;
import io.hypersistence.tsid.TSID;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
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

    private final long emitterTimeout;

    public SseEventService(long emitterTimeout) {
        this.emitterTimeout = emitterTimeout;
    }

    public SseEmitter createEventSender(@NotNull EnumSet<ServerEvent.Type> typesToListenOn) {
        final SseEmitter emitter = new SseEmitter(emitterTimeout);
        addEventSender(emitter, typesToListenOn);
        return emitter;
    }

    public void addEventSender(@NotNull SseEmitter emitter, @NotNull EnumSet<ServerEvent.Type> typesToListenOn) {
        //configure shutdown
        emitter.onCompletion(() -> {
            emitters.values().forEach(v -> v.remove(emitter));
            emitter.complete();
        });

        emitter.onTimeout(() -> {
            emitters.values().forEach(v -> v.remove(emitter));
            emitter.complete();
        });
        //add
        typesToListenOn.forEach(type -> emitters.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>()).add(emitter));
    }

    public void sendEvent(ServerEvent<?> event) {
        if (eventRunner == null)
            eventRunner = (EventRunner) SiriusJobs.runInBackground(new EventRunner());
//        System.out.println("Add Event to Queue: " + event); //todo remove debug
        events.add(event);
    }

    @Override
    public void shutdown() {
        if (eventRunner != null)
            eventRunner.shutdown();
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
//                        System.out.println("Retrieved event from Queue: " + eventData); //todo remove debug
                        for (SseEmitter emitter : emitters.getOrDefault(eventData.getEventType(), List.of())) {
                            try {
                                StringBuilder name = new StringBuilder()
                                        .append(eventData.getProjectId()).append(".")
                                        .append(eventData.getEventType().name());

                                if (eventData.getEventType() == ServerEvent.Type.JOB)
                                    name.append(".").append(((Job) eventData.getData()).getId());

                                emitter.send(SseEmitter.event()
                                        .data(eventData, MediaType.APPLICATION_JSON)
                                        .name(name.toString())
                                        .id(TSID.fast().toString()));
//                                System.out.println("Send event" + e.build().toString() + " to Client: " + emitter.toString()); //todo remove debug
                            } catch (IOException e) {
                                logDebug("Error when sending event to client!", e);
                                emitter.completeWithError(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                            }
                        }
//                        System.out.println(); //todo remove debug
//                        System.out.println(); //todo remove debug
                    } catch (InterruptedException e) {
                        if (eventData == ServerEvents.EMPTY_EVENT())
                            return true;
                        checkForInterruption();
                    }
                }
                // closing all connections to clients.
                emitters.values().stream().flatMap(Collection::stream).distinct().forEach(ResponseBodyEmitter::complete);
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        }

        public synchronized void shutdown() {
//        events.clear(); //do we want to clear event bevor shutdown for faster but less clean shutdown?
            events.add(ServerEvents.EMPTY_EVENT());
        }
    }
}