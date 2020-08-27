
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

import java.util.*;

/**
 * A (hierarchical structured) data document is an abstraction over YAML/JSON/XML/sonstewas.
 *
 * It consists of document entries (of type General) which are either numeric values, booleans, strings, lists or
 * dictionaries (a string{@literal ->}general map). The lists and dictionaries contain again document entries, therefore a
 * document is a tree which inner vertices are lists or dictionaries and which leafs are java primitive types or strings.
 */
public abstract class DataDocument<General, Dictionary, List> {

    public static <Gen1, Dict1, List1, Gen2, Dict2, List2> Gen2 transform(DataDocument<Gen1, Dict1, List1> from,
                                                                          DataDocument<Gen2, Dict2, List2> to, Gen1 value) {
        if (from.equals(to)) return (Gen2)value;
        if (from.isInteger(value)) return to.wrap(from.getInt(value));
        if (from.isDouble(value)) return to.wrap(from.getDouble(value));
        if (from.isBoolean(value)) return to.wrap(from.getBoolean(value));
        if (from.isString(value)) return to.wrap(from.getString(value));
        if (from.isDictionary(value)) {
            final Dict1 dict1 = from.getDictionary(value);
            final Dict2 dict2 = to.newDictionary();
            final Iterator<Map.Entry<String, Gen1>> iter = from.iteratorOfDictionary(dict1);
            while (iter.hasNext()) {
                final Map.Entry<String, Gen1> entry = iter.next();
                to.addToDictionary(dict2, entry.getKey(), transform(from, to, entry.getValue()));
            }
            return to.wrapDictionary(dict2);
        }
        if (from.isList(value)) {
            final List1 list1 = from.getList(value);
            final List2 list2 = to.newList();
            final Iterator<Gen1> iter = from.iteratorOfList(list1);
            while (iter.hasNext()) to.addToList(list2, transform(from, to, iter.next()));
            return to.wrapList(list2);
        }
        if (from.isNull(value)) return to.getNull();
        throw new TypeError("Unknown type of '" + value + "'");
    }

    public Object convertToJava(General document) {
        return(transform(this, new JDKDocument(), document));
    }

    public abstract boolean isDictionary(General document);

    public abstract boolean isList(General document);

    public abstract boolean isInteger(General document);

    /**
     * @param document
     * @return true if the document can be converted to a double
     */
    public abstract boolean isDouble(General document);

    public abstract boolean isBoolean(General document);

    public abstract boolean isString(General document);

    public abstract boolean isNull(General document);

    public abstract long getInt(General document);

    public abstract double getDouble(General document);

    public abstract boolean getBoolean(General document);

    public abstract String getString(General document);

    public abstract Dictionary getDictionary(General document);

    public abstract List getList(General document);

    public abstract void addToList(List list, General value);
    public abstract General getFromList(List list, int index);
    public abstract void setInList(List list, int index, General value);

    public abstract General wrap(long value);
    public abstract General wrap(double value);
    public abstract General wrap(boolean value);
    public abstract General getNull();
    public abstract General wrap(String value);
    public abstract General wrapDictionary(Dictionary dict);
    public abstract General wrapList(List dict);

    public abstract Dictionary newDictionary();
    public abstract List newList();


    public void addToList(List list, long value) {
        addToList(list, wrap(value));
    }
    public void addToList(List list, double value) {
        addToList(list, wrap(value));
    }
    public void addToList(List list, boolean value) {
        addToList(list, wrap(value));
    }
    public void addToList(List list, String value) {
        addToList(list, wrap(value));
    }
    public void addDictionaryToList(List list, Dictionary value) {
        addToList(list, wrapDictionary(value));
    }
    public void addListToList(List list, List value) {
        addToList(list, wrapList(value));
    }
    public void addNullToList(List list) {
        addToList(list, getNull());
    }

    public long getIntFromList(List list, int index) {
        final General value = getFromList(list, index);
        if (!isInteger(value)) throw new TypeError("Can't convert '" + value + "' to integer");
        return getInt(value);
    }
    public double getDoubleFromList(List list, int index) {
        final General value = getFromList(list, index);
        if (!isDouble(value)) throw new TypeError("Can't convert '" + value + "' to double");
        return getDouble(value);
    }
    public boolean getBooleanFromList(List list, int index) {
        final General value = getFromList(list, index);
        if (!isBoolean(value)) throw new TypeError("Can't convert '" + value + "' to boolean");
        return getBoolean(value);
    }
    public String getStringFromList(List list, int index) {
        final General value = getFromList(list, index);
        if (!isString(value)) throw new TypeError("Can't convert '" + value + "' to string");
        return getString(value);
    }
    public Dictionary getDictionaryFromList(List list, int index) {
        final General value = getFromList(list, index);
        if (!isDictionary(value)) throw new TypeError("Can't convert '" + value + "' to dictionary");
        return getDictionary(value);
    }
    public List getListFromList(List list, int index) {
        final General value = getFromList(list, index);
        if (!isList(value)) throw new TypeError("Can't convert '" + value + "' to list");
        return getList(value);
    }

    public abstract General getFromDictionary(Dictionary dict, String key);

    public long getIntFromDictionary(Dictionary dict, String key) {
        final General value = getFromDictionary(dict, key);
        if (!isInteger(value)) throw new TypeError("Can't convert '" + value + "' to integer");
        return getInt(value);
    }
    public double getDoubleFromDictionary(Dictionary dict, String key) {
        final General value = getFromDictionary(dict, key);
        if (isInteger(value)) return getInt(value);
        if (isDouble(value)) return getDouble(value);
        try { // Compatibility layer: Sometimes non numeric values (e.g. Infinity, NaN) are saved as text. Nevertheless, try to parse it.
            if (isString(value))
                return Double.parseDouble(getString(value));
        } catch (NumberFormatException e) {
            throw new TypeError("Tried to convert Text to Double but couldn't convert '" + value + "'.", e);
        }
        throw new TypeError("Can't convert '" + value + "' to double");
    }
    public boolean getBooleanFromDictionary(Dictionary dict, String key) {
        final General value = getFromDictionary(dict, key);
        if (!isBoolean(value)) throw new TypeError("Can't convert '" + value + "' to boolean");
        return getBoolean(value);
    }
    public String getStringFromDictionary(Dictionary dict, String key) {
        final General value = getFromDictionary(dict, key);
        if (!isString(value)) throw new TypeError("Can't convert '" + value + "' to string");
        return getString(value);
    }
    public Dictionary getDictionaryFromDictionary(Dictionary dict, String key) {
        final General value = getFromDictionary(dict, key);
        if (!isDictionary(value)) throw new TypeError("Can't convert '" + value + "' to dictionary");
        return getDictionary(value);
    }
    public List getListFromDictionary(Dictionary dict, String key) {
        final General value = getFromDictionary(dict, key);
        if (!isList(value)) throw new TypeError("Can't convert '" + value + "' to list");
        return getList(value);
    }

    public void setInList(List list, int index, long value) {
        setInList(list, index, wrap(value));
    }

    public void setInList(List list, int index, double value) {
        setInList(list, index, wrap(value));
    }

    public void setInList(List list, int index, boolean value) {
        setInList(list, index, wrap(value));
    }

    public void setInList(List list, int index, String value) {
        setInList(list, index, wrap(value));
    }

    public void setDictionaryInList(List list, int index, Dictionary value) {
        setInList(list, index, wrapDictionary(value));
    }

    public void setListInList(List list, int index, List value) {
        setInList(list, index, wrapList(value));
    }

    public abstract General deleteFromList(List list, int index);

    public abstract void addToDictionary(Dictionary dict, String key, General value);

    public void addToDictionary(Dictionary dict, String key, long value) {
        addToDictionary(dict, key, wrap(value));
    }
    public void addToDictionary(Dictionary dict, String key, double value) {
        addToDictionary(dict, key, wrap(value));
    }
    public void addToDictionary(Dictionary dict, String key, String value) {
        addToDictionary(dict, key, wrap(value));
    }
    public void addToDictionary(Dictionary dict, String key, boolean value) {
        addToDictionary(dict, key, wrap(value));
    }
    public void addDictionaryToDictionary(Dictionary dict, String key, Dictionary value) {
        addToDictionary(dict, key, wrapDictionary(value));
    }
    public void addListToDictionary(Dictionary dict, String key, List value) {
        addToDictionary(dict, key, wrapList(value));
    }

    public boolean hasKeyInDictionary(Dictionary dict, String key) {
        return keySetOfDictionary(dict).contains(key);
    }

    public abstract General deleteFromDictionary(Dictionary dict, String key);

    public abstract Set<String> keySetOfDictionary(Dictionary dict);
    public abstract int sizeOfList(List list);

    public Iterator<General> iteratorOfList(final List list) {
        final int size = sizeOfList(list);
        return new Iterator<General>() {
            private int index=0;
            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public General next() {
                return getFromList(list, index++);
            }

            @Override
            public void remove() {
                if (index == 0) throw new IllegalStateException();
                deleteFromList(list, index-1);
            }
        };
    }

    public Iterator<Map.Entry<String, General>> iteratorOfDictionary(final Dictionary dict) {
        final Iterator<String> keys = keySetOfDictionary(dict).iterator();
        return new Iterator<Map.Entry<String, General>>() {
            String lastKey = null;
            @Override
            public boolean hasNext() {
                return keys.hasNext();
            }

            @Override
            public Map.Entry<String, General> next() {
                if (!hasNext()) throw new NoSuchElementException();
                lastKey = keys.next();
                return new AbstractMap.SimpleEntry<String, General>(lastKey, getFromDictionary(dict, lastKey));
            }

            @Override
            public void remove() {
                if (lastKey == null) throw new IllegalStateException();
                deleteFromDictionary(dict, lastKey);
            }
        };
    }

}
