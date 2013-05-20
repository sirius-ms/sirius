package de.unijena.bioinf.treeviewer;


import java.util.regex.Pattern;

public class Property {

    private final String name;
    private final int lineNumber;
    private final int offset, length;
    private final Pattern regexp;
    private boolean enabled;

    public Property(String name, int lineNumber, int offset, int length, Pattern regexp) {
        this.name = name;
        this.lineNumber = lineNumber;
        this.offset = offset;
        this.length = length;
        this.regexp = regexp;
        this.enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public Pattern getRegexp() {
        return regexp;
    }
}
