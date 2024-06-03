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

package de.unijena.bioinf.ChemistryBase.utils;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.AdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ms.annotations.SpectrumAnnotation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy.stringify;

public class SimpleSerializers {

    public static abstract class FromStringDeserializer<T> extends JsonDeserializer<T> {

        public abstract T getObject(String text);

        @Override
        public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            while (p.currentToken() != null && p.currentToken() != JsonToken.VALUE_STRING)
                p.nextToken();
            return getObject(p.getText());
        }

    }

    public static final class IonizationTypeDeserializer extends FromStringDeserializer<Ionization> {
        @Override
        public Ionization getObject(String text) {
            return Ionization.fromString(text);
        }
    }

    public static final class PrecursorIonTypeDeserializer extends FromStringDeserializer<PrecursorIonType> {
        @Override
        public PrecursorIonType getObject(String text) {
            return PrecursorIonType.fromString(text);
        }
    }

    public static final class MolecularFormulaDeserializer extends FromStringDeserializer<MolecularFormula> {
        @Override
        public MolecularFormula getObject(String text) {
            return MolecularFormula.parseOrThrow(text);
        }
    }

    public static final class CollisionEnergySerializer extends JsonSerializer<CollisionEnergy> {

        @Override
        public void serialize(CollisionEnergy value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

            try {
                String toSerialize;

                if (value.minEnergySource() == value.maxEnergySource())
                    toSerialize=stringify(value.minEnergySource()) + " eV";

               else
                toSerialize=stringify(value.minEnergySource()) + " - " + stringify(value.maxEnergySource()) + " eV";


                gen.writeString(toSerialize);



            }catch (RuntimeException e){
                throw new IOException();
            }
        }
    }

    public static final class CollisionEnergyDeserializer extends FromStringDeserializer<CollisionEnergy> {
        @Override
        public CollisionEnergy getObject(String text) {
            return CollisionEnergy.fromString(text);
        }
    }

    public static final class MSInstrumentationDeserializer extends FromStringDeserializer<MsInstrumentation> {
        @Override
        public MsInstrumentation getObject(String text) {
            return MsInstrumentation.Instrument.valueOf(text);
        }
    }

    public static final class AnnotationSerializer extends JsonSerializer<AdditionalFields> {

        @Override
        public void serialize(AdditionalFields value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            try {
                gen.writeStartObject();
                value.forEach((key, val) -> {
                    try {
                        gen.writeStringField(key, val);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                gen.writeEndObject();
            } catch (RuntimeException e) {
                throw new IOException(e);
            }
        }
    }

    public static final class AnnotationDeserializer extends JsonDeserializer<SpectrumAnnotation> {

        @Override
        public SpectrumAnnotation deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            JsonToken token = p.currentToken();
            if (token != JsonToken.START_OBJECT)
                return null;

            Map<String, String> map = new HashMap<>();
            for (token = p.nextToken(); token == JsonToken.FIELD_NAME; token = p.nextToken()) {
                String key = p.getCurrentName();
                String value = p.nextTextValue();
                map.put(key, value);
            }
            AdditionalFields af = new AdditionalFields();
            af.putAll(map);
            return af;
        }
    }

    public static final class DeviationDeserializer extends FromStringDeserializer<Deviation> {

        @Override
        public Deviation getObject(String text) {
            return Deviation.fromString(text);
        }

    }

}
