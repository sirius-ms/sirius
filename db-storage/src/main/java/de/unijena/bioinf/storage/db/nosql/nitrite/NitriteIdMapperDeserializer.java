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

package de.unijena.bioinf.storage.db.nosql.nitrite;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import java.io.IOException;
import java.lang.reflect.Field;

public class NitriteIdMapperDeserializer<T> extends JsonDeserializer<T> {

    protected final JsonDeserializer<T> deserializer;

    protected final Field idField;

    public NitriteIdMapperDeserializer(Class<T> clazz, String idField, JsonDeserializer<T> deserializer) throws NoSuchFieldException {
        super();
        this.deserializer = deserializer;
        this.idField = clazz.getDeclaredField(idField);
        this.idField.setAccessible(true);
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try (TokenBuffer buffer = new TokenBuffer(p.getCodec(), false)) {
            long id = -1L;

            JsonToken token = p.currentToken();
            while (!token.isStructStart()) {
                token = p.nextToken();
            }
            buffer.writeStartObject();

            for (token = p.nextToken(); token != null  && !token.isStructEnd(); token = p.nextToken()) {
                if (token == JsonToken.FIELD_NAME) {
                    String currentName = p.currentName();
                    if ("_id".equals(currentName) || idField.getName().equals(currentName)) {
                        id = p.nextLongValue(-1L);
                    } else {
                        buffer.copyCurrentStructure(p);
                    }
                } else {
                    buffer.copyCurrentStructure(p);
                }
            }
            buffer.writeEndObject();

            T value = deserializer.deserialize(buffer.asParserOnFirstToken(), ctxt);

            try {
                if (this.idField.getType().isPrimitive()) {
                    this.idField.setLong(value, id);
                } else {
                    this.idField.set(value, id);
                }
            } catch (IllegalAccessException e) {
                throw new IOException(e);
            }

            return value;
        }
    }

}
