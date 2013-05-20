package de.unijena.bioinf.treeviewer;


import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class DotMemory extends DotSource {

    private final String content;

    public DotMemory(String name, String content) {
        super(name);
        this.content = content;
    }

    @Override
    public Reader getContent() throws IOException {
        return new StringReader(content);
    }

    @Override
    public String getSource() {
        return getName();
    }
}
