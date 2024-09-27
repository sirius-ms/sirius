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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataObjectEvent<Data>  {

    protected final Data data;
    protected final String lastEventId;
    protected final DataEventType dataType;


    @JsonCreator
    public DataObjectEvent(@NotNull @JsonProperty("dataType") DataEventType dataType, @JsonProperty("data") Data data, @Nullable @JsonProperty("lastEventId") String lastEventId) {
        this.data = data;
        this.lastEventId = lastEventId;
        this.dataType = dataType;
    }

    public String getLastEventId() {
        return lastEventId;
    }

    public Data getData() {
        return data;
    }

    public DataEventType getDataType() {
        return dataType;
    }

    @Override
    public String toString() {
        return "DataObjectEvent{" +
                "data=" + data +
                ", lastEventId='" + lastEventId + '\'' +
                ", dataType=" + dataType +
                '}';
    }
}
