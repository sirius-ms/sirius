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

package de.unijena.bioinf.spectraldb.entities;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.SpectrumAnnotation;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

import java.io.IOException;

public class SimpleSpectrumSerializer extends JsonSerializer<SimpleSpectrum> {

    protected static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void serialize(SimpleSpectrum value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        DoubleList masses = new DoubleArrayList();
        DoubleList intensities = new DoubleArrayList();
        for (Peak p : value) {
            masses.add(p.getMass());
            intensities.add(p.getIntensity());
        }
        gen.writeObjectField("masses", masses.toDoubleArray());
        gen.writeObjectField("intensities", intensities.toDoubleArray());

        gen.writeFieldName("annotations");
        gen.writeStartObject();
        Annotated.Annotations<SpectrumAnnotation> annotations = value.annotations();
        annotations.forEach((clazz, annotation) -> {
            try {
                gen.writeStringField(clazz.getName(), mapper.writeValueAsString(annotation));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        gen.writeEndObject();
        gen.writeEndObject();
    }

}
