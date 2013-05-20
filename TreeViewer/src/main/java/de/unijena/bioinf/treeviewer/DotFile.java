package de.unijena.bioinf.treeviewer;

import java.io.*;


public class DotFile extends DotSource {

    private final File file;

    public DotFile(File file) {
        super(beautifyName(file));
        this.file = file;
    }

    @Override
    public Reader getContent() throws IOException{
        return new FileReader(file);
    }

    public static String beautifyName(File f) {
        if (f.getAbsolutePath().startsWith("/tmp")) {
            return f.getName().replaceFirst("\\d+\\.(?:dot|gv)$", "");
        } else {
            final String s = f.getName();
            final int i = f.getName().lastIndexOf('.');
            if (i >= 0)
                return s.substring(0, i);
            else return s;
        }
    }

    @Override
    public String getSource() {
        return file.getAbsolutePath();
    }
}
