package de.unijena.bioinf.fingerid;

import java.awt.*;

public class DatabaseLabel implements Comparable<DatabaseLabel> {

    protected final String name;
    protected final String[] values;
    protected final Rectangle rect;

    public DatabaseLabel(String name, String[] values, Rectangle rect) {
        this.name = name;
        this.values = values;
        this.rect = rect;
    }

    @Override
    public int compareTo(DatabaseLabel o) {
        return name.compareTo(o.name);
    }
}
