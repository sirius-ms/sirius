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

package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.ms.middleware.model.events.ServerEvent;
import de.unijena.bioinf.ms.middleware.service.events.EventService;
import de.unijena.bioinf.ms.middleware.service.events.SseEventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.EnumSet;

@RestController
@Tag(name = "Server-Sent Event (SSE)", description = "Server-Sent Event Controller that allows to listen to different types of changes.")
public class SseController {
    private final SseEventService sseEventService;

    public SseController(@Autowired EventService<?> eventService) {
        this.sseEventService = (SseEventService) eventService;
    }

    @GetMapping(path = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter listenToEvents(@RequestParam(defaultValue = "JOB,PROJECT") EnumSet<ServerEvent.Type> eventsToListenOn) {
        if (eventsToListenOn == null || eventsToListenOn.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event types to listen on must not be empty or null!");
        return sseEventService.createEventSender(eventsToListenOn);
    }
}
