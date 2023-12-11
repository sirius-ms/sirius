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

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import de.unijena.bioinf.storage.db.nosql.utils.ExtFieldUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.objects.Id;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Metadata {

    final public Map<Class<?>, JsonSerializer<?>> serializers = new HashMap<>();

    final public Map<Class<?>, JsonDeserializer<?>> deserializers = new HashMap<>();

    final public Map<Class<?>, Index[]> repoIndices = new HashMap<>();

    final public Map<Class<?>, String[]> optionalRepoFields = new HashMap<>();

    final public Map<Class<?>, Pair<String, Boolean>> idFields = new HashMap<>();

    final public Map<String, Index[]> collectionIndices = new HashMap<>();

    final public Map<String, String[]> optionalCollectionFields = new HashMap<>();

    private Metadata() {
    }

    public static Metadata build() {
        return new Metadata();
    }

    public <T> Metadata addRepository(
            Class<T> clazz,
            Index... indices
    ) throws IOException {
        String id = findIdField(clazz);
        if (id != null)
            addRepository(clazz, id, indices);
        else
            this.repoIndices.put(clazz, indices);
        return this;
    }

    public <T> Metadata addRepository(
            Class<T> clazz,
            String idField,
            Index... indices
    ) throws IOException {
        return addRepository(clazz, idField, false, indices);
    }

    public <T> Metadata addRepository(
            Class<T> clazz,
            String idField,
            boolean forceGenerateID,
            Index... indices
    ) throws IOException {
        validateIdField(clazz, idField);
        this.idFields.put(clazz, Pair.of(idField, forceGenerateID));
        this.repoIndices.put(clazz, indices);
        return this;
    }


    public <T> Metadata addSerialization(
            Class<T> clazz,
            JsonSerializer<T> jsonSerializer,
            JsonDeserializer<T> jsonDeserializer
    ) {
        addSerializer(clazz, jsonSerializer);
        addDeserializer(clazz, jsonDeserializer);
        return this;
    }

    public <T> Metadata addSerializer(
            Class<T> clazz,
            JsonSerializer<T> jsonSerializer
    ) {
        this.serializers.put(clazz, jsonSerializer);
        return this;
    }

    public <T> Metadata addDeserializer(
            Class<T> clazz,
            JsonDeserializer<T> jsonDeserializer
    ) {
        this.deserializers.put(clazz, jsonDeserializer);
        return this;
    }

    public <T> Metadata setOptionalFields(
            Class<T> clazz,
            String... fieldNames
    ) {
        this.optionalRepoFields.put(clazz, fieldNames);
        return this;
    }

    public Metadata addCollection(
            String name,
            Index... indices
    ) {
        this.collectionIndices.put(name, indices);
        return this;
    }

    public Metadata setOptionalFields(
            String collection,
            String... fieldNames
    ) {
        this.optionalCollectionFields.put(collection, fieldNames);
        return this;
    }

    private static String findIdField(Class<?> clazz) {
        return Arrays.stream(FieldUtils.getAllFields(clazz))
                .filter(f -> f.getAnnotationsByType(Id.class).length
                        + f.getAnnotationsByType(jakarta.persistence.Id.class).length > 0)
                .findFirst().map(Field::getName).orElse(null);
    }

    private static void validateIdField(Class<?> clazz, String idField) throws IOException {
        clazz.getModule().addOpens(clazz.getPackageName(), Metadata.class.getModule());
        Field field = ExtFieldUtils.getAllField(clazz, idField);
        field.setAccessible(true);
        if (!(Long.class.equals(field.getType()) || long.class.equals(field.getType()) || NitriteId.class.equals(field.getType()))) {
            throw new IOException(idField + " in " + clazz + " must be long or Long!");
        }
    }

}
