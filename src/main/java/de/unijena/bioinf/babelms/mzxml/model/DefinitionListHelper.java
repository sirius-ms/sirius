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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DefinitionListHelper {

    public interface Applicable {
        public DefinitionListHelper buildDefinitionList(DefinitionListHelper helper);
    }

    private StringBuilder buffer;
    private Map<String, Object> properties;
    private boolean skip;

    public DefinitionListHelper() {
        this.buffer = new StringBuilder();
        this.properties = new HashMap<String, Object>();
        this.skip = false;
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Skips all following appendings if property map contains the given key.
     * Remark that DefinitionListHelper doesn't support nested conditions,
     * so call this method and the belonging #{endCondition} in the same scope
     * @param key
     * @return
     */
    public DefinitionListHelper skipIf(String key) {
        skip = skip || (properties.containsKey(key));
        return this;
    }

    /**
     * Skips all following appendings if property map contains the given key
     * and the value of this key is equal to the given value.
     * Remark that DefinitionListHelper doesn't support nested conditions,
     * so call this method and the belonging #{endCondition} in the same scope
     * @param key
     * @param value
     * @return
     */
    public DefinitionListHelper skipIf(String key, Object value) {
        skip = skip || (value.equals(properties.get(key)));
        return this;
    }

    /**
     * Finish the effect of #{skipIf(String)} and #{skipIf(String, Object)}
     * @return
     */
    public DefinitionListHelper endCondition() {
        skip = false;
        return this;
    }

    public DefinitionListHelper startList() {
        if (skip) return this;
        buffer.append("<table border=\"0\" cellspacing=\"5px\">");
        return this;
    }

    public DefinitionListHelper endList() {
        if (skip) return this;
        buffer.append("</table>");
        return this;
    }

    public DefinitionListHelper append(Object o) {
        if (skip) return this;
        if (o == null) return this;
        if (o instanceof Applicable) {
            ((Applicable) o).buildDefinitionList(this);
        } else {
            buffer.append(o);
        }
        return this;
    }

    public DefinitionListHelper def(String name, Object value) {
        if (skip) return this;
        if (value == null) return this;
        buffer.append("<tr>");
        buffer.append("<td nowrap valign=\"top\">").append(name).append("</td>");
        if (value instanceof Applicable) {
           // buffer.append("<dd>");
           ((Applicable)value).buildDefinitionList(this);
           // buffer.append("</dd>");
        } else {
            buffer.append("<td nowrap>").append(value).append("</td>");
        }
        buffer.append("</tr>");
        return this;
    }

    public <T> DefinitionListHelper defEnumOf(String name, Iterable<T> c) {
        if (skip) return this;
        final Iterator<T> iter =  c.iterator();
        if (!iter.hasNext()) return this;
        
        //buffer.append("<td nowrap>").append(name).append("</td>").append("<td>");
       // startEnum();
       // buffer.append("<td colspan=\"2\">");
        while (iter.hasNext()) {
        	buffer.append("<tr>");
            list(iter.next());
            buffer.append("</tr>");
        }
       // endEnum();
       // buffer.append("</td>");
        
        return this;
    }

    public <K, V> DefinitionListHelper defListOf(String name, Map<K, V> m) {
        if (skip) return this;
        if (m == null || m.isEmpty()) return this;
        buffer.append("<dt>").append(name).append("</dt>");
        //startList();
        for (Map.Entry<K, V> entry : m.entrySet()) {
            def(entry.getKey().toString(), entry.getValue());
        }
        //endList();
        //buffer.append("</dd>");
        return this;
    }

    public String html() {
        return buffer.toString();
    }
     
    public DefinitionListHelper startEnum() {
        if (skip) return this;
        buffer.append("<dl>"); 
        return this;
    }
    public DefinitionListHelper endEnum() {
        if (skip) return this;
        buffer.append("</dl>");
        return this;
    }
    public DefinitionListHelper list(Object value) {
        if (skip) return this;
        if (value == null) return this;
        buffer.append("<td valign=\"top\">");
        if (value instanceof Applicable) {
            //buffer.append("<dd>listapp ");
            ((Applicable)value).buildDefinitionList(this);
           // buffer.append(" /listapp</dd>");
        } else {
            buffer.append(value);
        }
        buffer.append("</td>");
        return this;
    }

    public <T> DefinitionListHelper listAll(Iterable<T> value) {
        if (skip) return this;
        for (T v : value) {
            list(v);
        }
        return this;
    }

}
