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

package io.sirius.ms.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.Flow;

public class FluxToFlowBroadcast implements Closeable {

    private final Map<String, List<Flow.Subscriber<DataObjectEvent<?>>>> subscribers = new HashMap<>();

    private final ObjectMapper objectMapper;

    public FluxToFlowBroadcast(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized void unSubscribe(Flow.Subscriber<DataObjectEvent<?>> subscriber) {
        subscribers.values().forEach(v -> v.remove(subscriber));
    }

    public void subscribe(Flow.Subscriber<DataObjectEvent<?>> subscriber, @NotNull EnumSet<DataEventType> eventsToListenOn) {
        subscribe(subscriber, null, eventsToListenOn);
    }

    public void subscribe(Flow.Subscriber<DataObjectEvent<?>> subscriber, @Nullable String projectId, @NotNull EnumSet<DataEventType> eventsToListenOn) {
        subscribe(subscriber, null, projectId, eventsToListenOn);
    }

    public void subscribeToJob(Flow.Subscriber<DataObjectEvent<?>> subscriber, @NotNull String jobId, @NotNull String projectId) {
        subscribe(subscriber, jobId, projectId, EnumSet.of(DataEventType.JOB));
    }

    private synchronized void subscribe(Flow.Subscriber<DataObjectEvent<?>> subscriber, @Nullable String jobId, @Nullable String projectId, @NotNull EnumSet<DataEventType> eventsToListenOn) {
        if (eventsToListenOn.isEmpty())
            throw new IllegalArgumentException("events to listen on must not be empty");

        if (projectId != null)
            // filter by specific project
            eventsToListenOn.forEach(e -> {
                StringBuilder b = new StringBuilder().append(projectId).append(".").append(e.name());
                // filter by specific job
                if (jobId != null && !jobId.isBlank() && e == DataEventType.JOB)
                    b.append(".").append(jobId);
                subscribers.computeIfAbsent(b.toString(), k -> new ArrayList<>()).add(subscriber);
            });
        else
            // listen to events from all projects
            eventsToListenOn.forEach(e -> subscribers.computeIfAbsent(e.name(), k -> new ArrayList<>()).add(subscriber));


        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
            }
        });
    }

    public synchronized void onNext(@NotNull ServerSentEvent<String> sse) {
        final String[] evtSplit = Optional.ofNullable(sse.event()).map(s -> s.split("[.]")).filter(a -> a.length > 1).orElse(null);
        if (evtSplit == null || !DataObjectEvents.isKnownObjectDataType(evtSplit[1])) {
            LoggerFactory.getLogger(getClass()).warn("Skipping unknown sse event! {}", Arrays.toString(evtSplit));
            return;
        }

        final String evtType = evtSplit[1];
        DataObjectEvent<?> event = DataObjectEvents.fromJsonData(evtType, sse.data(), objectMapper);

        //broadcast with project.type.job filter or project.type filter
        subscribers.getOrDefault(sse.event(), List.of()).stream().toList().forEach(e -> {
            if (e instanceof PropertyChangeSubscriber)
                ((PropertyChangeSubscriber) e).onNext(evtType, event);
            else
                e.onNext(event);
        });

        if (evtSplit.length > 2 || event.dataType == DataEventType.JOB) {
            //broadcast with project filter only if event is a job event
            subscribers.getOrDefault(evtSplit[0] + "." + evtSplit[1], List.of()).stream().toList().forEach(e -> {
                if (e instanceof PropertyChangeSubscriber)
                    ((PropertyChangeSubscriber) e).onNext(evtType, event);
                else
                    e.onNext(event);
            });
        }

        //broadcast without additional filter, just by type
        subscribers.getOrDefault(evtType, List.of()).stream().toList().forEach(e -> {
            if (e instanceof PropertyChangeSubscriber)
                ((PropertyChangeSubscriber) e).onNext(evtType, event);
            else
                e.onNext(event);
        });


    }

    public synchronized void onError(Throwable throwable) {
        subscribers.values().stream().flatMap(Collection::stream).toList().forEach(e -> e.onError(throwable));
    }

    public synchronized void onComplete() {
        subscribers.values().stream().flatMap(Collection::stream).toList().forEach((Flow.Subscriber::onComplete));
    }

    @Override
    public synchronized void close() {
        onComplete();
        subscribers.clear();
    }
}
