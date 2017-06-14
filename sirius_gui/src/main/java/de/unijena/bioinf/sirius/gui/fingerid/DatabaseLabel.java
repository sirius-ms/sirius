package de.unijena.bioinf.sirius.gui.fingerid;

import java.awt.*;

public class DatabaseLabel implements Comparable<DatabaseLabel> {

    protected String name;
    protected String[] values;
    protected Rectangle rect;

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
