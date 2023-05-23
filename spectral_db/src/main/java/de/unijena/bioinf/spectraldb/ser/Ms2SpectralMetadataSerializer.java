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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumFileSource;
import de.unijena.bioinf.ChemistryBase.ms.Splash;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralMetadata;
import org.apache.commons.lang3.ClassUtils;

import java.io.IOException;

public class Ms2SpectralMetadataSerializer extends JsonSerializer<Ms2SpectralMetadata> {

        protected static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void serialize(Ms2SpectralMetadata value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeNumberField("peaksId", value.getPeaksId());

        gen.writeStringField("precursorIonType", value.getPrecursorIonType().toString());
        gen.writeNumberField("ionMass", value.getIonMass());
        if (value.getFormula() != null) {
            gen.writeStringField("formula", value.getFormula().toString());
        }
        gen.writeStringField("name", value.getName());
        gen.writeStringField("smiles", value.getSmiles());
        if (value.getSpectralDbLink() != null) {
            gen.writeStringField("linkName", value.getSpectralDbLink().name);
            gen.writeStringField("linkID", value.getSpectralDbLink().id);
        }

        if (value.getDbLinks() != null && !value.getDbLinks().isEmpty()) {
            gen.writeFieldName("dbLinks");
            gen.writeStartArray();
            for (DBLink link : value.getDbLinks()) {
                gen.writeString(link.name);
                gen.writeString(link.id);
            }
            gen.writeEndArray();
        }

        try {
            gen.writeFieldName("annotations");
            gen.writeStartObject();
            value.annotations().forEach((clazz, annotation) -> {
                try {
                    gen.writeStringField(clazz.getName(), mapper.writeValueAsString(annotation));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            gen.writeEndObject();

            gen.writeFieldName("experimentAnnotations");
            gen.writeStartObject();
            value.experimentAnnotations().forEach((clazz, annotation) -> {
                System.out.println(clazz);
                try {
                    if (InChI.class.equals(clazz)) {
                        gen.writeArrayFieldStart(annotation.getIdentifier());
                        gen.writeString(((InChI) annotation).key);
                        gen.writeString(((InChI) annotation).in3D);
                        gen.writeEndArray();
                    } else if (MsInstrumentation.class.equals(clazz) || ClassUtils.getAllInterfaces(clazz).contains(MsInstrumentation.class)) {
                        gen.writeStringField(annotation.getIdentifier(), ((MsInstrumentation.Instrument) annotation).name());
                    } else if (SpectrumFileSource.class.equals(clazz)) {
                        gen.writeStringField(annotation.getIdentifier(), ((SpectrumFileSource) annotation).value.toString());
                    } else if (Splash.class.equals(clazz)) {
                        gen.writeStringField(annotation.getIdentifier(), ((Splash) annotation).getSplash());
                    } else {
                        gen.writeStringField(annotation.getIdentifier(), annotation.toString());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            gen.writeEndObject();
        } catch (RuntimeException e) {
            throw new IOException(e);
        }

        gen.writeFieldName("header");
        gen.writeStartObject();
        gen.writeNumberField("precursorMz", value.getPrecursorMz());
        gen.writeStringField("collisionEnergy", value.getCollisionEnergy().toString());
        gen.writeNumberField("totalIonCount", value.getTotalIonCount());
        gen.writeStringField("ionization", value.getIonization().toString());
        gen.writeNumberField("msLevel", value.getMsLevel());
        gen.writeNumberField("scanNumber", value.getScanNumber());
        gen.writeEndObject();

        gen.writeEndObject();
    }

}
