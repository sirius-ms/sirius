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
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Supplier;

public class Metadata {

    final public Map<Class<?>, JsonSerializer<?>> serializers = new HashMap<>();

    final public Map<Class<?>, JsonDeserializer<?>> deserializers = new HashMap<>();

    final public Map<Class<?>, Index[]> repoIndices = new HashMap<>();

    final public Map<Class<?>, String[]> optionalRepoFields = new HashMap<>();

    final public Map<Class<?>, Field> pkFields = new HashMap<>();

    final public Map<Class<?>, Supplier<?>> pkSuppliers = new HashMap<>();

    final public Map<String, Index[]> collectionIndices = new HashMap<>();

    final public Map<String, String[]> optionalCollectionFields = new HashMap<>();

    private static final List<Class<?>> ALLOWED_PRIMARY_KEYS = List.of(
            Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
            Boolean.class, Character.class, Date.class, java.sql.Date.class, BigDecimal.class, BigInteger.class, String.class
    );

    private Metadata() {
    }

    public static Metadata build() {
        return new Metadata();
    }

    public <T> Metadata addRepository(
            Class<T> clazz,
            Index... indices
    ) throws IOException {
        Field pkField = findAndValidatePrimaryKeyField(clazz);
        this.pkFields.put(clazz, pkField);
        Set<Index> ind = new LinkedHashSet<>(List.of(indices));
        ind.add(new Index(IndexType.UNIQUE, pkField.getName()));
        this.repoIndices.put(clazz, ind.toArray(Index[]::new));
        return this;
    }

    public <T> Metadata addRepository(
            Class<T> clazz,
            String pkFieldName,
            Index... indices
    ) throws IOException {
        Field pkField = findAndValidatePrimaryKeyFieldByName(clazz, pkFieldName);
        this.pkFields.put(clazz, pkField);
        Set<Index> ind = new LinkedHashSet<>(List.of(indices));
        ind.add(new Index(IndexType.UNIQUE, pkFieldName));
        this.repoIndices.put(clazz, ind.toArray(Index[]::new));
        return this;
    }

    public <T> Metadata addPrimaryKeySupplier(
            Class<T> clazz,
            Supplier<?> supplier
    ) {
        this.pkSuppliers.put(clazz, supplier);
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

    private static Field findAndValidatePrimaryKeyField(@NotNull Class<?> clazz) throws IOException {
        List<Field> pkFields = FieldUtils.getFieldsListWithAnnotation(clazz, jakarta.persistence.Id.class);
        if (pkFields.isEmpty()) {
            throw new IOException(clazz + " has no primary key. The primary key must be annotated with jakarta.persistence.Id!");
        } else if (pkFields.size() > 1) {
            throw new IOException(clazz + " has multiple primary keys. Only one primary key is allowed!");
        }
        Field pkField = pkFields.get(0);
        return validatePrimaryKeyField(clazz, pkField);
    }

    private static Field findAndValidatePrimaryKeyFieldByName(@NotNull Class<?> clazz, @NotNull String fieldName) throws IOException {
        List<Field> pkFields = FieldUtils.getAllFieldsList(clazz).stream().filter(f -> Objects.equals(f.getName(), fieldName)).toList();
        if (pkFields.isEmpty()) {
            throw new IOException(clazz + " has no primary key. The primary key must be annotated with jakarta.persistence.Id!");
        } else if (pkFields.size() > 1) {
            throw new IOException(clazz + " has multiple primary keys. Only one primary key is allowed!");
        }
        Field pkField = pkFields.get(0);
        return validatePrimaryKeyField(clazz, pkField);
    }

    @NotNull
    private static Field validatePrimaryKeyField(@NotNull Class<?> clazz, @NotNull Field pkField) throws IOException {
        Class<?> pkType = pkField.getType();
        if (pkType.isPrimitive() || ALLOWED_PRIMARY_KEYS.contains(pkType)) {
            return pkField;
        } else {
            throw new IOException(clazz + " has an invalid primary key type. Allowed are: any Java primitive type; any primitive wrapper type; String; java.util.Date; java.sql.Date; java.math.BigDecimal; java.math.BigInteger.");
        }
    }

}
