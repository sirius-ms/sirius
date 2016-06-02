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

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.math.BigDecimal;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

public class JSONDocumentType extends DataDocument<Object, JSONObject, JSONArray> {

    public static void writeParameters(Object o, Writer out) throws IOException {
        final ParameterHelper helper = ParameterHelper.getParameterHelper();
        final JSONDocumentType json = new JSONDocumentType();
        final Object value = helper.wrap(json, o);
        writeJson(json, value, out);
    }

    public static <G, D, L> void writeJson(DataDocument<G, D, L> document, G value, Writer out) throws IOException {
        final JSONDocumentType json = new JSONDocumentType();
        final Object object = DataDocument.transform(document, json, value);
        try {
            out.write(JSONObject.valueToString(object));
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public static JSONObject readFromFile(File fileName) throws IOException {
        final FileReader reader = new FileReader(fileName);
        final JSONObject o = read(reader);
        reader.close();
        return o;
    }

    public static JSONObject read(Reader reader) throws IOException {
        try {
            return new JSONObject(new JSONTokener(reader));
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public static JSONObject getJSON(String cpName, String fileName) throws IOException {
        // 1. check for resource with same name
        final InputStream stream = JSONDocumentType.class.getResourceAsStream(cpName);
        if (stream != null) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            try {
                final JSONObject obj = JSONDocumentType.read(reader);
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
    public boolean isDictionary(Object document) {
        return document instanceof JSONObject;
    }

    @Override
    public boolean isList(Object document) {
        return document instanceof JSONArray;
    }

    @Override
    public boolean isInteger(Object document) {
        return (document instanceof Number) && !(document instanceof Float || document instanceof Double || document instanceof BigDecimal);
    }

    @Override
    public boolean isDouble(Object document) {
        return (document instanceof Number);
    }

    @Override
    public boolean isBoolean(Object document) {
        return document instanceof Boolean;
    }

    @Override
    public boolean isString(Object document) {
        return document instanceof String;
    }

    @Override
    public boolean isNull(Object document) {
        return document == null;
    }

    @Override
    public long getInt(Object document) {
        return ((Number)document).longValue();
    }

    @Override
    public double getDouble(Object document) {
        return ((Number)document).doubleValue();
    }

    @Override
    public boolean getBoolean(Object document) {
        return (Boolean)document;
    }

    @Override
    public String getString(Object document) {
        return (String)document;
    }


    @Override
    public JSONObject getDictionary(Object document) {
        return (JSONObject)document;
    }

    @Override
    public JSONArray getList(Object document) {
        return (JSONArray)document;
    }

    @Override
    public void addToList(JSONArray jsonArray, Object value) {
        jsonArray.put(value);
    }

    @Override
    public Object getFromList(JSONArray jsonArray, int index) {
        try {
            return jsonArray.get(index);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setInList(JSONArray jsonArray, int index, Object value) {
        try {
            jsonArray.put(index, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object wrap(long value) {
        return value;
    }

    @Override
    public Object wrap(double value) {
        return value;
    }

    @Override
    public Object wrap(boolean value) {
        return value;
    }

    @Override
    public Object getNull() {
        return null;
    }

    @Override
    public Object wrap(String value) {
        return value;
    }

    @Override
    public Object wrapDictionary(JSONObject dict) {
        return dict;
    }

    @Override
    public Object wrapList(JSONArray dict) {
        return dict;
    }

    @Override
    public JSONObject newDictionary() {
        return new JSONObject();
    }

    @Override
    public JSONArray newList() {
        return new JSONArray();
    }

    @Override
    public Object getFromDictionary(JSONObject dict, String key) {
        try {
            return dict.get(key);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteFromList(JSONArray jsonArray, int index) {
        jsonArray.remove(index);
    }

    @Override
    public void addToDictionary(JSONObject dict, String key, Object value) {
        try {
            dict.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object deleteFromDictionary(JSONObject dict, String key) {
        return dict.remove(key);
    }

    @Override
    public Set<String> keySetOfDictionary(final JSONObject dict) {
        return new AbstractSet<String>() {
            @Override
            public Iterator<String> iterator() {
                return dict.keys();
            }

            @Override
            public int size() {
                return dict.length();
            }
        };
    }

    @Override
    public int sizeOfList(JSONArray jsonArray) {
        return jsonArray.length();
    }


}
