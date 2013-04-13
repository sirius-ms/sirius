/*
 * Sirius MassSpec Tool
 * based on the Epos Framework
 * Copyright (C) 2009.  University of Jena
 *
 * This file is part of Sirius.
 *
 * Sirius is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sirius is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Sirius.  If not, see <http://www.gnu.org/licenses/>;.
*/
package de.unijena.bioinf.babelms.mzxml.model;

import java.io.Serializable;
import java.util.*;

public class NameValueSet extends AbstractMap<String, String> implements Serializable, DefinitionListHelper.Applicable {

    private static final long serialVersionUID = 9024250692840105251L;

    private List<NameValuePair> keyValues;

    public NameValueSet() {
        this.keyValues = new ArrayList<NameValuePair>();
    }

    public DefinitionListHelper buildDefinitionList(DefinitionListHelper helper) {
        if (isEmpty()) return helper;
        helper.startList();
        for (NameValuePair pair : keyValues) {
            helper.def(pair.getName(), pair.getValue());
        }
        helper.endList();
        return helper;

    }

    public List<String> getAllValues(String key) {
        final ArrayList<String> values = new ArrayList<String>();
        for (NameValuePair pair : keyValues) {
            if (pair.getName().equals(key))
                values.add(pair.getValue());
        }
        return values;
    }

    public Iterator<NameValuePair> keyValueIterator() {
        return keyValues.iterator();
    }

    public void add(NameValuePair pair) {
        keyValues.add(pair);
    }

    public List<NameValuePair> getKeyValues() {
        return keyValues;
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return new AbstractSet<Entry<String, String>>() {
            @Override
            public Iterator<Entry<String, String>> iterator() {
                return new Iterator<Entry<String, String>>() {
                    Iterator<NameValuePair> iter = keyValues.iterator();

                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    public Entry<String, String> next() {
                        final NameValuePair pair = iter.next();
                        return new AbstractMap.SimpleImmutableEntry<String, String>(pair.getName(), pair.getValue());
                    }

                    public void remove() {
                        iter.remove();
                    }
                };
            }

            @Override
            public int size() {
                return keyValues.size();
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NameValueSet that = (NameValueSet) o;

        if (keyValues != null ? !keyValues.equals(that.keyValues) : that.keyValues != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (keyValues != null ? keyValues.hashCode() : 0);
        return result;
    }
}
