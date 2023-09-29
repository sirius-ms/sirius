
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

import java.io.*;
import java.util.HashSet;
import java.util.Set;


public class JSONDocumentType extends DataDocument<JsonNode, ObjectNode, ArrayNode> {
    private final static ObjectMapper mapper = new ObjectMapper();
    public static void writeParameters(Object o, Writer out) throws IOException {
        final ParameterHelper helper = ParameterHelper.getParameterHelper();
        final JSONDocumentType json = new JSONDocumentType();
        final JsonNode value = helper.wrap(json, o);
        writeJson(json, value, out);
    }

    public static <G, D, L> void writeJson(DataDocument<G, D, L> document, G value, Writer out) throws IOException {
        final JSONDocumentType json = new JSONDocumentType();
        final JsonNode object = DataDocument.transform(document, json, value);

        out.write(mapper.writeValueAsString(object));
    }

    public static JsonNode readFromFile(File fileName) throws IOException {
        final FileReader reader = new FileReader(fileName);
        final JsonNode o = read(reader);
        reader.close();
        return o;
    }

    public static JsonNode read(Reader reader) throws IOException {
        return mapper.readTree(reader);
    }

    public static JsonNode getJSON(String cpName, String fileName) throws IOException {
        // 1. check for resource with same name
        final InputStream stream = JSONDocumentType.class.getResourceAsStream(cpName);
        if (stream != null) {
            final BufferedReader reader = FileUtils.ensureBuffering(new InputStreamReader(stream));
            try {
                final JsonNode obj = JSONDocumentType.read(reader);
                return obj;
            } finally {
                reader.close();
            }
        } else {
            // 2. check for file
            return JSONDocumentType.readFromFile(new File(fileName));
        }
    }

    @Override
    public boolean isDictionary(JsonNode document) {
        return document.isObject();
    }

    @Override
    public boolean isList(JsonNode document) {
        return document.isArray();
    }

    public boolean isNumber(JsonNode document) {
        return document.isNumber();
    }

    @Override
    public double getDoubleFromList(ArrayNode jsonElements, int index) {
        return jsonElements.get(index).asDouble();
    }

    @Override
    public double getDoubleFromDictionary(ObjectNode dict, String key) {
       return getDoubleFromDictionary((JsonNode) dict, key);
    }

    public double getDoubleFromDictionary(JsonNode dict, String key) {
        final JsonNode elem = dict.get(key);
        if (elem == null) throw new NullPointerException("Key is not contained in dictionary: '" + key + "'\n");
        return elem.asDouble();
    }

    @Override
    public boolean isInteger(JsonNode document) {
        if (!document.isNumber()) return false;
        final double val = document.asDouble();
        return val == Math.floor(val) && !Double.isInfinite(val);
    }

    @Override
    public boolean isDouble(JsonNode document) {
        return document.isNumber();
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
        return document == null || document.isNull();
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
    public ObjectNode getDictionary(JsonNode document) {
        return (ObjectNode) document;
    }

    @Override
    public ArrayNode getList(JsonNode document) {
        return (ArrayNode) document;
    }

    @Override
    public void addToList(ArrayNode jsonArray, JsonNode value) {
        jsonArray.add(value);
    }

    @Override
    public JsonNode getFromList(ArrayNode jsonArray, int index) {
        return jsonArray.get(index);
    }

    @Override
    public void setInList(ArrayNode jsonArray, int index, JsonNode value) {
        jsonArray.set(index, value);
    }

    @Override
    public LongNode wrap(long value) {
        return LongNode.valueOf(value);
    }

    @Override
    public DoubleNode wrap(double value) {
        return DoubleNode.valueOf(value);
    }

    @Override
    public BooleanNode wrap(boolean value) {
        return BooleanNode.valueOf(value);
    }

    @Override
    public NullNode getNull() {
        return NullNode.instance;
    }

    @Override
    public TextNode wrap(String value) {
        return TextNode.valueOf(value);
    }

    @Override
    public ObjectNode wrapDictionary(ObjectNode dict) {
        return dict;
    }

    @Override
    public ArrayNode wrapList(ArrayNode dict) {
        return dict;
    }

    @Override
    public ObjectNode newDictionary() {
        return mapper.createObjectNode();
    }

    @Override
    public ArrayNode newList() {
        return mapper.createArrayNode();
    }

    @Override
    public JsonNode getFromDictionary(final ObjectNode dict, final String key) {
        return getFromDictionary((JsonNode) dict, key);
    }

    public JsonNode getFromDictionary(final JsonNode dict, final String key) {
        return dict.get(key);
    }

    @Override
    public JsonNode deleteFromList(final ArrayNode jsonArray, final int index) {
        return jsonArray.remove(index);
    }

    @Override
    public void addToDictionary(final ObjectNode dict, String key, JsonNode value) {
        dict.set(key, value);
    }

    @Override
    public JsonNode deleteFromDictionary(final ObjectNode dict, String key) {
        return dict.remove(key);
    }

    @Override
    public Set<String> keySetOfDictionary(final ObjectNode dict) {
       return keySetOfDictionary((JsonNode) dict);
    }

    public Set<String> keySetOfDictionary(final JsonNode dict) {
        Set<String> s = new HashSet<>(dict.size());
        dict.fieldNames().forEachRemaining(s::add);
        return s;
    }

    @Override
    public int sizeOfList(final ArrayNode jsonArray) {
        return jsonArray.size();
    }
}
