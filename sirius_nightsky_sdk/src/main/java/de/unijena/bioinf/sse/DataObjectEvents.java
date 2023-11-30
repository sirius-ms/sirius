/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ms.nightsky.sdk.model.GuiParameters;
import de.unijena.bioinf.ms.nightsky.sdk.model.Job;
import de.unijena.bioinf.ms.nightsky.sdk.model.ProjectChangeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public static Object fromJsonData(String eventName, String data, ObjectMapper jsonMapper) {
        if (isKnownObjectDataType(eventName))
            return fromJsonData(DataEventType.valueOf(eventName.toUpperCase()), data, jsonMapper);
        return data;
    }

    public static Object fromJsonData(@NotNull DataEventType dataType,
                                           @NotNull String data,
                                           @NotNull ObjectMapper jsonMapper) {
        try {
            switch (dataType) {
                case JOB -> {
                    return jsonMapper.readValue(data, Job.class);
                }
                case PROJECT -> {
                    return jsonMapper.readValue(data, ProjectChangeEvent.class);
                }
                case GUI_STATE -> {
                    return jsonMapper.readValue(data, GuiParameters.class);
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        throw new IllegalArgumentException("Unknown Data type: " + dataType);
    }
}
