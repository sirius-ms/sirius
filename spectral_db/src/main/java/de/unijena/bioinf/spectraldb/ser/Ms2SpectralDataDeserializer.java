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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralData;

import java.io.IOException;

public class Ms2SpectralDataDeserializer extends JsonDeserializer<Ms2SpectralData> {

    @Override
    public Ms2SpectralData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        long id = -1L;
        long metaId = -1L;
        double[] masses = null;
        double[] intensities = null;
        double ionMass = 0;
        double precursorMz = 0;

        JsonToken token = p.currentToken();
        while (!token.isStructStart()) {
            token = p.nextToken();
        }
        for (token = p.nextToken(); token != null && !token.isStructEnd(); token = p.nextToken()) {
            if (token == JsonToken.FIELD_NAME) {
                switch (p.currentName()) {
                    case "metaId":
                        metaId = p.nextLongValue(-1L);
                        break;
                    case "ionMass":
                        token = p.nextToken();
                        ionMass = p.getDoubleValue();
                        break;
                    case "precursorMz":
                        token = p.nextToken();
                        precursorMz = p.getDoubleValue();
                        break;
                    case "masses":
                        token = p.nextToken();
                        masses = p.readValueAs(double[].class);
                        break;
                    case "intensities":
                        token = p.nextToken();
                        intensities = p.readValueAs(double[].class);
                        break;
                }
            }
        }
        return new Ms2SpectralData(id, metaId, ionMass, precursorMz, masses, intensities);
    }

}


