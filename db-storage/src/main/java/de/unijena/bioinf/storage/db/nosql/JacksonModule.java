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

package de.unijena.bioinf.storage.db.nosql;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.lang.reflect.Field;

public class JacksonModule extends SimpleModule {

    public JacksonModule() {
        super("sirius-nitrite", Version.unknownVersion());
    }

    public static class IdMapperSerializer<T> extends JsonSerializer<T> {

        private final JsonSerializer<T> serializer;
        private final Field idField;

        public IdMapperSerializer(Class<T> clazz, String idField, JsonSerializer<T> serializer) throws NoSuchFieldException {
            this.serializer = serializer;
            this.idField = clazz.getDeclaredField(idField);
        }

        @Override
        public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            try {
                if (idField != null) {
                    if (idField.getType().isPrimitive() && idField.getLong(value) > -1L) {
                        gen.writeNumberField("_id", idField.getLong(value));
                    } else if (idField.get(value) != null && (Long) idField.get(value) > -1L) {
                        gen.writeNumberField("_id", (Long) idField.get(value));
                    }
                }
            } catch (IllegalAccessException e) {
                throw new IOException(e);
            }
            gen.writeFieldName("value");
            serializer.serialize(value, gen, serializers);
            gen.writeEndObject();
        }
    }

    public static class IdMapperDeserializer<T> extends JsonDeserializer<T> {

        private final JsonDeserializer<T> deserializer;
        private final Field idField;

        public IdMapperDeserializer(Class<T> clazz, String idField, JsonDeserializer<T> deserializer) throws NoSuchFieldException {
            this.deserializer = deserializer;
            this.idField = clazz.getDeclaredField(idField);
        }

        @Override
        public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            long id = -1L;
            T value = null;
            JsonToken token = p.currentToken();
            while (!token.isStructStart()) {
                token = p.nextToken();
            }
            while (token != null && !token.isStructEnd()) {
                token = p.nextToken();
                if (token == JsonToken.FIELD_NAME) {
                    switch (p.currentName()) {
                        case "_id":
                            id = p.nextLongValue(-1L);
                            break;
                        case "value":
                            value = deserializer.deserialize(p, ctxt);
                            break;
                    }
                }
            }
            if (value != null) {
                try {
                    if (this.idField.getType().isPrimitive()) {
                        this.idField.setLong(value, id);
                    } else {
                        this.idField.set(value, id);
                    }
                } catch (IllegalAccessException e) {
                    throw new IOException(e);
                }
            }
            return value;
        }
    }

}
