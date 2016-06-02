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
package de.unijena.bioinf.ChemistryBase.data;


import java.math.BigDecimal;
import java.util.*;

public class JDKDocument extends DataDocument<Object, Map<String, Object>, List<Object>> {

    @Override
    public boolean isDictionary(Object document) {
        return document instanceof Map;
    }

    @Override
    public boolean isList(Object document) {
        return document instanceof List;
    }

    @Override
    public boolean isInteger(Object document) {
        return (document instanceof Number) && !(document instanceof Float || document instanceof Double || document instanceof BigDecimal);
    }

    @Override
    public boolean isDouble(Object document) {
        return document instanceof Number;
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
    public Map<String, Object> getDictionary(Object document) {
        return (Map<String, Object>)document;
    }

    @Override
    public List<Object> getList(Object document) {
        return (List<Object>)document;
    }

    @Override
    public void addToList(List<Object> objects, Object value) {
        objects.add(value);
    }

    @Override
    public Object getFromList(List<Object> objects, int index) {
        return objects.get(index);
    }

    @Override
    public void setInList(List<Object> objects, int index, Object value) {
        objects.set(index, value);
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
    public Object wrapDictionary(Map<String, Object> dict) {
        return (Map<String, Object>)dict;
    }

    @Override
    public Object wrapList(List<Object> dict) {
        return (List<Object>)dict;
    }

    @Override
    public Map<String, Object> newDictionary() {
        return new HashMap<String, Object>();
    }

    @Override
    public List<Object> newList() {
        return new ArrayList<Object>();
    }

    @Override
    public Object getFromDictionary(Map<String, Object> dict, String key) {
        return dict.get(key);
    }

    @Override
    public void deleteFromList(List<Object> objects, int index) {
        objects.remove(index);
    }

    @Override
    public void addToDictionary(Map<String, Object> dict, String key, Object value) {
        dict.put(key, value);
    }

    @Override
    public Object deleteFromDictionary(Map<String, Object> dict, String key) {
        return dict.remove(key);
    }

    @Override
    public Set<String> keySetOfDictionary(Map<String, Object> dict) {
        return dict.keySet();
    }

    @Override
    public int sizeOfList(List<Object> objects) {
        return objects.size();
    }

    @Override
    public Iterator<Object> iteratorOfList(List<Object> objects) {
        return objects.iterator();
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iteratorOfDictionary(Map<String, Object> dict) {
        return dict.entrySet().iterator();
    }
}
