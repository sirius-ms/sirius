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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.dizitart.no2.NitriteId;

import java.io.IOException;
import java.lang.reflect.Field;

public class NitriteIdMapperSerializer<T> extends JsonSerializer<T> {

    protected final JsonSerializer<T> serializer;

    protected final Field idField;

    public NitriteIdMapperSerializer(Class<T> clazz, String idField, JsonSerializer<T> serializer) throws NoSuchFieldException {
        super();
        this.serializer = serializer;
        this.idField = clazz.getDeclaredField(idField);
        this.idField.setAccessible(true);
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        try {
            long idValue = -1L;
            if (idField.getType().isPrimitive()) {
                idValue = idField.getLong(value);
                if (idValue < 0) {
                    idValue = NitriteId.newId().getIdValue();
                    idField.setLong(value, idValue);
                }
            } else {
                idValue = (Long) idField.get(value);
                if (idValue < 0) {
                    idValue = NitriteId.newId().getIdValue();
                    idField.set(value, (Long) idValue);
                }
            }

            try (TokenBuffer buffer = new TokenBuffer(gen.getCodec(), false)) {
                serializer.serialize(value, buffer, serializers);
                JsonParser p = buffer.asParser();
                JsonToken token = p.nextToken();
                while (!token.isStructStart()) {
                    token = p.nextToken();
                }

                gen.writeStartObject();
                gen.writeNumberField("_id", idValue);
                gen.writeNumberField(idField.getName(), idValue);

                for (token = p.nextToken(); token != null; token = p.nextToken()) {
                    gen.copyCurrentStructure(p);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IOException(e);
        }
    }

}
