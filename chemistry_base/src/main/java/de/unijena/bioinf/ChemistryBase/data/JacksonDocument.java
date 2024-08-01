/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.data;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Cam be replaced by writable version JSONDocument which is know based on jackson
 */
@Deprecated(forRemoval = true)
public class JacksonDocument extends DataDocument<JsonNode, JsonNode, JsonNode> {

    public JsonNode fromReader(Reader r) throws IOException {
        JsonFactory factory = new JsonFactory();
        factory.enable(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS);
        ObjectMapper mapper = new ObjectMapper(factory);
        return mapper.readTree(r);
    }
    public JsonNode fromString(String r) throws IOException {
        JsonFactory factory = new JsonFactory();
        factory.enable(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS);
        ObjectMapper mapper = new ObjectMapper(factory);
        return mapper.readTree(r);
    }

    @Override
    public boolean isDictionary(JsonNode document) {
        return document.isObject();
    }

    @Override
    public boolean isList(JsonNode document) {
        return document.isArray();
    }

    @Override
    public boolean isInteger(JsonNode document) {
        return document.isIntegralNumber();
    }

    @Override
    public boolean isDouble(JsonNode document) {
        return document.isDouble();
    }

    @Override
    public boolean isBoolean(JsonNode document) {
        return document.isBoolean();
    }

    @Override
    public boolean isString(JsonNode document) {
        return document.isTextual();
    }

    @Override
    public boolean isNull(JsonNode document) {
        return document.isNull();
    }

    @Override
    public long getInt(JsonNode document) {
        return document.asInt();
    }

    @Override
    public double getDouble(JsonNode document) {
        return document.asDouble();
    }

    @Override
    public boolean getBoolean(JsonNode document) {
        return document.asBoolean();
    }

    @Override
    public String getString(JsonNode document) {
        return document.asText();
    }

    @Override
    public JsonNode getDictionary(JsonNode document) {
        return document;
    }

    @Override
    public JsonNode getList(JsonNode document) {
        return document;
    }

    @Override
    public void addToList(JsonNode jsonNode, JsonNode value) {
        throw new UnsupportedOperationException("JacksonDocument is immutable.");
    }

    @Override
    public JsonNode getFromList(JsonNode jsonNode, int index) {
        return jsonNode.get(index);
    }

    @Override
    public void setInList(JsonNode jsonNode, int index, JsonNode value) {
        throw new UnsupportedOperationException("JacksonDocument is immutable.");
    }

    @Override
    public JsonNode wrap(long value) {
        throw new UnsupportedOperationException("JacksonDocument is immutable.");
    }

    @Override
    public JsonNode wrap(double value) {
        throw new UnsupportedOperationException("JacksonDocument is immutable.");
    }

    @Override
    public JsonNode wrap(boolean value) {
        throw new UnsupportedOperationException("JacksonDocument is immutable.");
    }

    @Override
    public JsonNode getNull() {
        throw new UnsupportedOperationException("JacksonDocument is immutable.");
    }

    @Override
    public JsonNode wrap(String value) {
        throw new UnsupportedOperationException("JacksonDocument is immutable.");
    }

    @Override
    public JsonNode wrapDictionary(JsonNode dict) {
        return dict;
    }

    @Override
    public JsonNode wrapList(JsonNode dict) {
        return dict;
    }

    @Override
    public JsonNode newDictionary() {
        throw new UnsupportedOperationException("JacksonDocument is immutable.");
    }

    @Override
    public JsonNode newList() {
        throw new UnsupportedOperationException("JacksonDocument is immutable.");
    }

    @Override
    public JsonNode getFromDictionary(JsonNode dict, String key) {
        return dict.get(key);
    }

    @Override
    public JsonNode deleteFromList(JsonNode jsonNode, int index) {
        throw new UnsupportedOperationException("JacksonDocument is immutable.");
    }

    @Override
    public void addToDictionary(JsonNode dict, String key, JsonNode value) {
        throw new UnsupportedOperationException("JacksonDocument is immutable.");
    }

    @Override
    public JsonNode deleteFromDictionary(JsonNode dict, String key) {
        throw new UnsupportedOperationException("JacksonDocument is immutable.");
    }

    @Override
    public Set<String> keySetOfDictionary(JsonNode dict) {
        HashSet<String> set = new HashSet<>();
        for (Iterator<String> iter= dict.fieldNames(); iter.hasNext(); ) set.add(iter.next());
        return set;
    }

    @Override
    public int sizeOfList(JsonNode jsonNode) {
        return jsonNode.size();
    }
}
