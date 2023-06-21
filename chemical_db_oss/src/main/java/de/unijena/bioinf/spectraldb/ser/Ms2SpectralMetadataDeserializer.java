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

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralMetadata;

import java.io.IOException;

public class Ms2SpectralMetadataDeserializer extends JsonDeserializer<Ms2SpectralMetadata> {

    @Override
    public Ms2SpectralMetadata deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // FIXME DIRTY, DIRTY HACK
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(PrecursorIonType.class, new JsonDeserializer<>() {
            @Override
            public PrecursorIonType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                while (p.currentToken() != JsonToken.VALUE_STRING)
                    p.nextToken();
                return PrecursorIonType.fromString(p.getText());
            }
        });
        module.addDeserializer(MolecularFormula.class, new JsonDeserializer<>() {
            @Override
            public MolecularFormula deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                while (p.currentToken() != JsonToken.VALUE_STRING)
                    p.nextToken();
                return MolecularFormula.parseOrThrow(p.getText());
            }
        });
        module.addDeserializer(CollisionEnergy.class, new JsonDeserializer<>() {
            @Override
            public CollisionEnergy deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                while (p.currentToken() != JsonToken.VALUE_STRING)
                    p.nextToken();
                return CollisionEnergy.fromString(p.getText());
            }
        });
        module.addDeserializer(MsInstrumentation.class, new JsonDeserializer<>() {
            @Override
            public MsInstrumentation deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                while (p.currentToken() != JsonToken.VALUE_STRING)
                    p.nextToken();
                return MsInstrumentation.Instrument.valueOf(p.getText());
            }
        });
        mapper.registerModule(module);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // END DIRTY, DIRTY HACK
        return mapper.readValue(p, Ms2SpectralMetadata.class);
    }


}
