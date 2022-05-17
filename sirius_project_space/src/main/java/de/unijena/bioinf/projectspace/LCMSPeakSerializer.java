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

package de.unijena.bioinf.projectspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class LCMSPeakSerializer implements ComponentSerializer<CompoundContainerId, CompoundContainer, LCMSPeakInformation> {

    @Nullable
    @Override
    public LCMSPeakInformation read(ProjectReader reader, CompoundContainerId id, CompoundContainer container) throws IOException {
        if (reader.exists(SiriusLocations.LCMS_JSON)) {
            return reader.binaryFile(SiriusLocations.LCMS_JSON, (io)->{
                try (GZIPInputStream zipped = new GZIPInputStream(io)) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.readValue(zipped, LCMSPeakInformation.class);
                }
            });
        } else {
            return LCMSPeakInformation.empty();
        }
    }

    @Override
    public void write(ProjectWriter writer, CompoundContainerId container, CompoundContainer id, Optional<LCMSPeakInformation> component) throws IOException {
        writer.binaryFile(SiriusLocations.LCMS_JSON, bufferedOutputStream -> {
            try (GZIPOutputStream zipped = new GZIPOutputStream(bufferedOutputStream)) {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(zipped, component.orElseGet(()->new LCMSPeakInformation(new CoelutingTraceSet[0])));
            }
        });
    }

    @Override
    public void delete(ProjectWriter writer, CompoundContainerId id) throws IOException {
        writer.delete(SiriusLocations.LCMS_JSON);
    }
}
