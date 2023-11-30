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

package de.unijena.bioinf.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;

import java.io.Closeable;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;

public class FluxToFlowBroadcast implements Closeable {

    private final MultiValueMap<String, Flow.Subscriber<Object>> subscribers = new MultiValueMapAdapter<>(new ConcurrentHashMap<>());

    private final ObjectMapper objectMapper;

    public FluxToFlowBroadcast(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void unSubscribe(Flow.Subscriber<Object> subscriber) {
        subscribers.values().forEach(v -> v.remove(subscriber));
    }
    public void subscribe(Flow.Subscriber<Object> subscriber, @Nullable String projectId, @NotNull EnumSet<DataEventType> eventsToListenOn) {
        if (eventsToListenOn.isEmpty())
            throw new IllegalArgumentException("events to listen on must not be empty");

        if (projectId != null)
            // filter by specific project
            eventsToListenOn.forEach(e -> subscribers.add(projectId + "." + e.name(), subscriber));
        else
            // listen to events from all projects
            eventsToListenOn.forEach(e -> subscribers.add(e.name(), subscriber));

        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {}

            @Override
            public void cancel() {}
        });
    }

    public void onNext(@NotNull ServerSentEvent<String> sse) {
        if (!Optional.ofNullable(sse.event()).map(s -> s.split("[.]")).filter(a -> a.length > 1)
                .map(a -> a[1]).map(DataObjectEvents::isKnownObjectDataType).orElse(false))
            return;

        Object data = DataObjectEvents.fromJsonData(sse.event(), sse.data(), objectMapper);
        final String evt = sse.event().split("[.]")[1];

        //broadcast with project filter
        subscribers.getOrDefault(sse.event(), List.of()).forEach(e -> {
            if (e instanceof PropertyChangeSubscriber)
                ((PropertyChangeSubscriber)e).onNext(evt , data);
            else
                e.onNext(data);
        });

        //broadcast without project filter
        subscribers.getOrDefault(evt, List.of()).forEach(e -> {
            if (e instanceof PropertyChangeSubscriber)
                ((PropertyChangeSubscriber)e).onNext(evt , data);
            else
                e.onNext(data);
        });
    }

    public void onError(Throwable throwable) {
        subscribers.values().stream().flatMap(Collection::stream).distinct().forEach(e -> e.onError(throwable));
    }

    public void onComplete() {
        subscribers.values().stream().flatMap(Collection::stream).distinct().forEach(Flow.Subscriber::onComplete);
    }

    @Override
    public synchronized void close() {
        onComplete();
        subscribers.clear();
    }
}
