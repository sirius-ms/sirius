package de.unijena.bioinf.treeviewer.dot;

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
