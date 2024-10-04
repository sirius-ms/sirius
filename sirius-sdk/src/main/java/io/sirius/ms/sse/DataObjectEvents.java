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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sirius.ms.sdk.model.BackgroundComputationsStateEvent;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.ProjectChangeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DataObjectEvents {

    public static boolean isKnownObjectDataType(@Nullable String eventName) {
        if (eventName == null || eventName.isBlank())
            return false;
        try {
            DataEventType.valueOf(eventName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    public static <Data> Optional<Data> toDataObjectEventData(Object probableDataObjectEvent, Class<Data> dataType) {
        return toDataObjectEvent(probableDataObjectEvent, dataType).map(DataObjectEvent::getData);
    }
    public static <Data> Optional<DataObjectEvent<Data>> toDataObjectEvent(Object probableDataObjectEvent, Class<Data> dataType) {
        if (probableDataObjectEvent instanceof DataObjectEvent<?>) {
            if( dataType.isAssignableFrom(((DataObjectEvent<?>) probableDataObjectEvent).getData().getClass())){
                return Optional.of(((DataObjectEvent<Data>) probableDataObjectEvent));
            }
        }
        return Optional.empty();
    }

    public static DataObjectEvent<?> fromJsonData(String eventName, String data, ObjectMapper jsonMapper) {
        if (isKnownObjectDataType(eventName))
            return fromJsonData(DataEventType.valueOf(eventName.toUpperCase()), data, jsonMapper);
        try {
            return jsonMapper.readValue(data, new TypeReference<DataObjectEvent<String>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static DataObjectEvent<?> fromJsonData(@NotNull DataEventType dataType,
                                           @NotNull String data,
                                           @NotNull ObjectMapper jsonMapper) {
        try {
            switch (dataType) {
                case JOB -> {
                    return jsonMapper.readValue(data, new TypeReference<DataObjectEvent<Job>>() {});
                }
                case PROJECT -> {
                    return jsonMapper.readValue(data, new TypeReference<DataObjectEvent<ProjectChangeEvent>>() {});
                }
                case BACKGROUND_COMPUTATIONS_STATE -> {
                    return jsonMapper.readValue(data, new TypeReference<DataObjectEvent<BackgroundComputationsStateEvent>>() {});
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        throw new IllegalArgumentException("Unknown Data type: " + dataType);
    }
}
