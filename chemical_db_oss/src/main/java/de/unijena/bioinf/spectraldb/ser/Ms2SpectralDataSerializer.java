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

package de.unijena.bioinf.spectraldb.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralData;

import java.io.IOException;

public class Ms2SpectralDataSerializer extends JsonSerializer<Ms2SpectralData> {

    @Override
    public void serialize(Ms2SpectralData value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeNumberField("metaId", value.getMetaId());
        gen.writeNumberField("precursorMz", value.getPrecursorMz());
        gen.writeObjectField("masses", value.getMasses());
        gen.writeObjectField("intensities", value.getIntensities());

        gen.writeEndObject();
    }

}
