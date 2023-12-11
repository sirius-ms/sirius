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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.BasicSerializerFactory;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.type.ClassKey;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import de.unijena.bioinf.storage.db.nosql.utils.ExtFieldUtils;
import org.dizitart.no2.NitriteId;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

public class NitriteIdMapperSerializer<T> extends JsonSerializer<T> {

    protected final NitriteDatabase database;

    protected ObjectMapper objectMapper;

    protected final JsonSerializer<T> jsonSerializer;

    protected final Class<T> clazz;

    protected final Field idField;

    protected final boolean force;

    public NitriteIdMapperSerializer(Class<T> clazz, String idField, boolean forceGenerateID, NitriteDatabase database) {
        super();
        this.database = database;
        this.jsonSerializer = null;
        this.force = forceGenerateID;
        this.clazz = clazz;
        this.idField = ExtFieldUtils.getAllField(clazz, idField);
        this.idField.setAccessible(true);
    }

    public NitriteIdMapperSerializer(Class<T> clazz, String idField, boolean forceGenerateID, JsonSerializer<T> jsonSerializer) {
        super();
        this.database = null;
        this.jsonSerializer = jsonSerializer;
        this.force = forceGenerateID;
        this.clazz = clazz;
        this.idField = ExtFieldUtils.getAllField(clazz, idField);
        this.idField.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    private <T> void addSerializer(SimpleSerializers sd, Class<?> cls, JsonSerializer<?> jsonSerializer) {
        Class<T> c = (Class<T>) cls;
        JsonSerializer<T> jd = (JsonSerializer<T>) jsonSerializer;
        sd.addSerializer(c, jd);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        try {
            long idValue;
            if (idField.getType().isPrimitive()) {
                idValue = idField.getLong(value);
                if (idValue <= 0 || force) {
                    idValue = NitriteId.newId().getIdValue();
                    idField.setLong(value, idValue);
                }
            } else {
                idValue = idField.get(value) != null? (Long) idField.get(value) : -1L;
                if (idValue <= 0 || force) {
                    idValue = NitriteId.newId().getIdValue();
                    idField.set(value, idValue);
                }
            }

            try (TokenBuffer buffer = new TokenBuffer(gen.getCodec(), false)) {
                if (database != null) {
                    if (objectMapper == null) {
                        ObjectMapper originalMapper = database.getJacksonMapper().getObjectMapper();
                        objectMapper = originalMapper.copy();

                        SerializerFactoryConfig originalConfig = ((BasicSerializerFactory) originalMapper.getSerializerFactory()).getFactoryConfig();
                        SerializerFactoryConfig copyConfig = new SerializerFactoryConfig();
                        for (Serializers s : originalConfig.serializers()) {
                            if (s.findSerializer(null, originalMapper.getTypeFactory().constructType(clazz), null) == null) {
                                copyConfig = copyConfig.withAdditionalSerializers(s);
                            } else {
                                Field mapField = SimpleSerializers.class.getDeclaredField("_classMappings");
                                mapField.setAccessible(true);
                                Map<ClassKey, JsonSerializer<?>> classMap = (Map<ClassKey, JsonSerializer<?>>) mapField.get(s);
                                SimpleSerializers copyS = new SimpleSerializers();
                                ClassKey key = new ClassKey(clazz);
                                for (Map.Entry<ClassKey, JsonSerializer<?>> entry : classMap.entrySet()) {
                                    ClassKey ckey = entry.getKey();
                                    JsonSerializer<?> v = entry.getValue();
                                    if (!ckey.equals(key)) {
                                        try {
                                            addSerializer(copyS, Class.forName(ckey.toString()), v);
                                        } catch (ClassNotFoundException e) {
                                            throw new IOException(e);
                                        }
                                    }
                                }
                                copyConfig = copyConfig.withAdditionalSerializers(copyS);
                            }
                        }
                        objectMapper.setSerializerFactory(BeanSerializerFactory.instance.withConfig(copyConfig));
                    }

                    objectMapper.writeValue(buffer, value);
                } else {
                    jsonSerializer.serialize(value, buffer, serializers);
                }

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
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IOException(e);
        }
    }

}
