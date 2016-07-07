package de.unijena.bioinf.sirius.gui.fingerid;

import java.awt.*;

public class DatabaseLabel {

    protected String name;
    protected String[] values;
    protected Rectangle rect;

    public DatabaseLabel(String name, String[] values, Rectangle rect) {
        this.name = name;
        this.values = values;
        this.rect = rect;
    }
}
