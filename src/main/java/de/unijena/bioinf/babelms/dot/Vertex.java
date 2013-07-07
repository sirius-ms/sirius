package de.unijena.bioinf.babelms.dot;

import java.util.*;

public class Vertex {

    private final String name;
    private final HashMap<String, String> properties;
    private BitSet invisible;

    public Vertex(String name, HashMap<String, String> props) {
        this.name = name;
        this.properties = new HashMap<String, String>(props);
        this.invisible = new BitSet();
    }

    public BitSet getInvisible() {
        return invisible;
    }

    public Vertex(String name) {
        this.name = name;
        this.properties = new HashMap<String, String>();
        this.invisible = new BitSet();
    }
    public Vertex(Vertex v) {
        this.name = v.getName();
        this.properties = new HashMap<String, String>(v.getProperties());
        this.invisible = (BitSet) v.getInvisible().clone();
    }

    public String getName() {
        return name;
    }

    public HashMap<String, String> getProperties() {
        return properties;
    }

    public String hideDisabledLabels() {
        final String label = properties.get("label");
        if (invisible.isEmpty()) return label;
        final String[] names = label.split("\\\\n");
        final List<String> filtered = new ArrayList<String>(names.length);
        for (int i=0; i < names.length; ++i) {
            if (!invisible.get(i)) filtered.add(names[i]);
        }
        final StringBuilder buf = new StringBuilder();
        for (int i=0; i< filtered.size(); ++i) {
            buf.append(filtered.get(i));
            if (i+1 < filtered.size()) {
                buf.append("\\n");
            }
        }
        return buf.toString();
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append(name);
        b.append(" [");
        final Iterator<Map.Entry<String, String>> iter = properties.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            b.append(entry.getKey()).append("=\"");
            final String value = (entry.getKey().equalsIgnoreCase("label") ? hideDisabledLabels() : entry.getValue());
            b.append(value.replace("\"", "\\\""));
            b.append("\"");
            if (iter.hasNext()) b.append(", ");
        }
        b.append("];");
        return b.toString();
    }
}
