package de.unijena.bioinf.treeviewer;

import java.io.IOException;
import java.io.Reader;

public abstract class DotSource {

    private final String name;

    protected DotSource(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract Reader getContent() throws IOException;

    @Override
    public String toString() {
        return name;
    }

    public abstract String getSource();
}
