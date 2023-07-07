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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.BasicDeserializerFactory;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.type.ClassKey;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

public class NitriteIdMapperDeserializer<T> extends JsonDeserializer<T> {

    protected final NitriteDatabase database;

    protected ObjectMapper objectMapper;

    protected final JsonDeserializer<T> jsonDeserializer;


    protected final Class<T> clazz;

    protected final Field idField;

    public NitriteIdMapperDeserializer(Class<T> clazz, String idField, NitriteDatabase database) throws NoSuchFieldException {
        super();
        this.database = database;
        this.jsonDeserializer = null;
        this.clazz = clazz;
        this.idField = clazz.getDeclaredField(idField);
        this.idField.setAccessible(true);
    }

    public NitriteIdMapperDeserializer(Class<T> clazz, String idField, JsonDeserializer<T> jsonDeserializer) throws NoSuchFieldException {
        super();
        this.database = null;
        this.jsonDeserializer = jsonDeserializer;
        this.clazz = clazz;
        this.idField = clazz.getDeclaredField(idField);
        this.idField.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    private <T> void addDeserializer(SimpleDeserializers sd, Class<?> cls, JsonDeserializer<?> jsonDeserializer) {
        Class<T> c = (Class<T>) cls;
        JsonDeserializer<T> jd = (JsonDeserializer<T>) jsonDeserializer;
        sd.addDeserializer(c, jd);
    }

    @SuppressWarnings("unchecked")
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

            T value;
            if (database != null) {
                if (objectMapper == null) {
                    ObjectMapper originalMapper = database.getJacksonMapper().getObjectMapper();
                    objectMapper = originalMapper.copy();

                    DeserializerFactoryConfig originalConfig = ((BasicDeserializerFactory) originalMapper.getDeserializationContext().getFactory()).getFactoryConfig();
                    DeserializerFactoryConfig copyConfig = new DeserializerFactoryConfig();
                    for (Deserializers d : originalConfig.deserializers()) {
                        if (d.findBeanDeserializer(originalMapper.constructType(clazz), null, null) == null) {
                            copyConfig = copyConfig.withAdditionalDeserializers(d);
                        } else {
                                Field mapField = SimpleDeserializers.class.getDeclaredField("_classMappings");
                                mapField.setAccessible(true);
                                Map<ClassKey, JsonDeserializer<?>> classMap = (Map<ClassKey, JsonDeserializer<?>>) mapField.get(d);
                                SimpleDeserializers copyD = new SimpleDeserializers();
                                ClassKey key = new ClassKey(clazz);
                                for (Map.Entry<ClassKey, JsonDeserializer<?>> entry : classMap.entrySet()) {
                                    ClassKey ckey = entry.getKey();
                                    JsonDeserializer<?> v = entry.getValue();
                                    if (!ckey.equals(key)) {
                                        try {
                                            addDeserializer(copyD, Class.forName(ckey.toString()), v);
                                        } catch (ClassNotFoundException e) {
                                            throw new IOException(e);
                                        }
                                    }
                                }
                                copyConfig = copyConfig.withAdditionalDeserializers(copyD);
                        }
                    }
                    BasicDeserializerFactory copyFactory = (BasicDeserializerFactory) BeanDeserializerFactory.instance.withConfig(copyConfig);
                    DefaultDeserializationContext copyContext = ((DefaultDeserializationContext) originalMapper.getDeserializationContext()).with(copyFactory);

                    Field ctxtField = ObjectMapper.class.getDeclaredField("_deserializationContext");
                    ctxtField.setAccessible(true);
                    ctxtField.set(objectMapper, copyContext);
                }

                value = objectMapper.readValue(buffer.asParserOnFirstToken(), clazz);
            } else {
                value = jsonDeserializer.deserialize(buffer.asParserOnFirstToken(), ctxt);
            }

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
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
