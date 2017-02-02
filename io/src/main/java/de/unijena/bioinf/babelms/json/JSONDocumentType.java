/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.babelms.json;

import com.google.gson.*;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class JSONDocumentType extends DataDocument<JsonElement, JsonObject, JsonArray> {
    private final static JsonParser PARSER =  new JsonParser();


    public static void writeParameters(Object o, Writer out) throws IOException {
        final ParameterHelper helper = ParameterHelper.getParameterHelper();
        final JSONDocumentType json = new JSONDocumentType();
        final JsonElement value = helper.wrap(json, o);
        writeJson(json, value, out);
    }

    public static <G, D, L> void writeJson(DataDocument<G, D, L> document, G value, Writer out) throws IOException {
        final JSONDocumentType json = new JSONDocumentType();
        final JsonElement object = DataDocument.transform(document, json, value);

        out.write(object.toString()); //write to JSON string
    }

    public static JsonObject readFromFile(File fileName) throws IOException {
        final FileReader reader = new FileReader(fileName);
        final JsonObject o = read(reader);
        reader.close();
        return o;
    }

    public static JsonObject read(Reader reader) {
        return PARSER.parse(reader).getAsJsonObject();
    }

    public static JsonObject getJSON(String cpName, String fileName) throws IOException {
        // 1. check for resource with same name
        final InputStream stream = JSONDocumentType.class.getResourceAsStream(cpName);
        if (stream != null) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            try {
                final JsonObject obj = JSONDocumentType.read(reader);
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
    public boolean isDictionary(JsonElement document) {
        return document.isJsonObject();
    }

    @Override
    public boolean isList(JsonElement document) {
        return document.isJsonArray();
    }

    public boolean isNumber(JsonElement document) {
        if (!document.isJsonPrimitive())
            return false;
        return document.getAsJsonPrimitive().isNumber();
    }

    @Override
    public double getDoubleFromList(JsonArray jsonElements, int index) {
        return jsonElements.get(index).getAsDouble();
    }

    @Override
    public double getDoubleFromDictionary(JsonObject dict, String key) {
        return dict.get(key).getAsDouble();
    }

    @Override
    public boolean isInteger(JsonElement document) {
        if (!document.isJsonPrimitive()) return false;
        final JsonPrimitive primitive = (JsonPrimitive)document;
        if (!primitive.isNumber()) return false;
        final BigDecimal dec = document.getAsBigDecimal();
        if (dec.scale() > 0) return false;
        final BigInteger i = dec.toBigInteger();
        return i.bitLength() <= 31;
    }

    @Override
    public boolean isDouble(JsonElement document) {
        return isNumber(document);/* && document.getAsBigDecimal().scale() >= 0; */ // TODO: What does this mean?
    }

    @Override
    public boolean isBoolean(JsonElement document) {
        return document.isJsonPrimitive() && document.getAsJsonPrimitive().isBoolean();
    }

    @Override
    public boolean isString(JsonElement document) {
        return document.isJsonPrimitive() && document.getAsJsonPrimitive().isString();
    }

    @Override
    public boolean isNull(JsonElement document) {
        return document == null || document.isJsonNull();
    }

    @Override
    public long getInt(JsonElement document) {
        return document.getAsInt();
    }

    @Override
    public double getDouble(JsonElement document) {
        return document.getAsDouble();
    }

    @Override
    public boolean getBoolean(JsonElement document) {
        return document.getAsBoolean();
    }

    @Override
    public String getString(JsonElement document) {
        return document.getAsString();
    }


    @Override
    public JsonObject getDictionary(JsonElement document) {
        return document.getAsJsonObject();
    }

    @Override
    public JsonArray getList(JsonElement document) {
        return document.getAsJsonArray();
    }

    @Override
    public void addToList(JsonArray jsonArray, JsonElement value) {
        jsonArray.add(value);
    }

    @Override
    public JsonElement getFromList(JsonArray jsonArray, int index) {
        return jsonArray.get(index);
    }

    @Override
    public void setInList(JsonArray jsonArray, int index, JsonElement value) {
        jsonArray.set(index, value);
    }

    @Override
    public JsonPrimitive wrap(long value) {
        return wrap((Number)value);
    }

    @Override
    public JsonPrimitive wrap(double value) {
        return wrap((Number)value);
    }

    public JsonPrimitive wrap(Number value) {
        return new JsonPrimitive(value);
    }

    @Override
    public JsonPrimitive wrap(boolean value) {
        return new JsonPrimitive(value);
    }

    @Override
    public JsonNull getNull() {
        return JsonNull.INSTANCE;
    }

    @Override
    public JsonPrimitive wrap(String value) {
        return new JsonPrimitive(value);
    }

    @Override
    public JsonObject wrapDictionary(JsonObject dict) {
        return dict;
    }

    @Override
    public JsonArray wrapList(JsonArray dict) {
        return dict;
    }

    @Override
    public JsonObject newDictionary() {
        return new JsonObject();
    }

    @Override
    public JsonArray newList() {
        return new JsonArray();
    }

    @Override
    public JsonElement getFromDictionary(final JsonObject dict, final String key) {
        return dict.get(key);
    }

    @Override
    public JsonElement deleteFromList(final JsonArray jsonArray, final int index) {
        return jsonArray.remove(index);
    }

    @Override
    public void addToDictionary(final JsonObject dict, String key, JsonElement value) {
        dict.add(key, value);
    }

    @Override
    public JsonElement deleteFromDictionary(final JsonObject dict, String key) {
        return dict.remove(key);
    }

    @Override
    public Set<String> keySetOfDictionary(final JsonObject dict) {
        Set<Map.Entry<String, JsonElement>> e = dict.entrySet();
        Set<String> s = new HashSet<>(e.size());
        for (Map.Entry<String, JsonElement> entry : e) {
            s.add(entry.getKey());
        }
        return s;
    }

    @Override
    public int sizeOfList(final JsonArray jsonArray) {
        return jsonArray.size();
    }


}
