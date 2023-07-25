/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import de.unijena.bioinf.spectraldb.entities.SimpleSerializers;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SpectralSearchResultSerializer implements ComponentSerializer<CompoundContainerId, CompoundContainer, SpectralSearchResult> {

    private final static ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Deviation.class, new ToStringSerializer());
        module.addDeserializer(Deviation.class, new SimpleSerializers.DeviationDeserializer());
        objectMapper.registerModule(module);
    }

    @Override
    public @Nullable SpectralSearchResult read(ProjectReader reader, CompoundContainerId id, CompoundContainer container) throws IOException {
        if (reader.exists(SiriusLocations.SPECTRAL_SEARCH_JSON)) {
            return reader.binaryFile(SiriusLocations.SPECTRAL_SEARCH_JSON, (io)->{
                try (GZIPInputStream zipped = new GZIPInputStream(io)) {
                    return objectMapper.readValue(zipped, SpectralSearchResult.class);
                }
            });
        }
        return null;
    }

    @Override
    public void write(ProjectWriter writer, CompoundContainerId id, CompoundContainer container, Optional<SpectralSearchResult> component) throws IOException {
        if (component.isPresent()) {
            writer.binaryFile(SiriusLocations.SPECTRAL_SEARCH_JSON, bufferedOutputStream -> {
                try (GZIPOutputStream zipped = new GZIPOutputStream(bufferedOutputStream)) {
                    objectMapper.writeValue(zipped, component.get());
                }
            });
        }
    }

    @Override
    public void delete(ProjectWriter writer, CompoundContainerId id) throws IOException {
        writer.delete(SiriusLocations.SPECTRAL_SEARCH_JSON);
    }

}
