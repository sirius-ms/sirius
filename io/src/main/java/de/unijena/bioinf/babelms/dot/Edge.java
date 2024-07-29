
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

package de.unijena.bioinf.babelms.dot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Edge {

    private final String u, v;
    private final HashMap<String, String> properties;

    public Edge(String u, String v) {
        this.u = u;
        this.v = v;
        this.properties = new HashMap<String, String>();
    }

    public Edge(Edge e) {
        this.u = e.u;
        this.v = e.v;
        this.properties = new HashMap<String, String>(e.getProperties());
    }

    public Edge(String u, String v, HashMap<String, String> props) {
        this.u = u;
        this.v = v;
        this.properties = new HashMap<String, String>(props);
    }

    public String getHead() {
        return u;
    }

    public String getTail() {
        return v;
    }

    public HashMap<String, String> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append(u).append(" -> ").append(v);
        b.append(" [");
        final Iterator<Map.Entry<String, String>> iter = properties.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            b.append(entry.getKey()).append("=\"");
            final String value = entry.getValue().replace("\"", "\\\"");
            b.append(value);
            b.append("\"");
            if (iter.hasNext()) b.append(", ");
        }
        b.append("];");
        return b.toString();
    }

}
