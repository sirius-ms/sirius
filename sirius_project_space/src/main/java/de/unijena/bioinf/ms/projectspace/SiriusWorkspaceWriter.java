package de.unijena.bioinf.ms.projectspace;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SiriusWorkspaceWriter implements DirectoryWriter.WritingEnvironment {

    protected ZipOutputStream zip;
    protected List<String> pathElements;

    public SiriusWorkspaceWriter(File file) throws FileNotFoundException {
        this(new FileOutputStream(file));
    }

    public SiriusWorkspaceWriter(OutputStream stream) {
        this.zip = new ZipOutputStream(stream, Charset.forName("UTF-8"));
        this.pathElements = new ArrayList<>();
    }

    @Override
    public void enterDirectory(String name) {
        pathElements.add(name);
        try {
            zip.putNextEntry(new ZipEntry(join(pathElements)));
            zip.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String join(List<String> pathElements) {
        StringBuilder buf = new StringBuilder(pathElements.size()*8);
        for (String p : pathElements) buf.append(p).append('/');
        return buf.toString();
    }

    @Override
    public OutputStream openFile(String name) {
        try {
            zip.putNextEntry(new ZipEntry(join(pathElements) + name));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return zip;
    }

    @Override
    public void closeFile() {
        try {
            zip.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void leaveDirectory() {
        pathElements.remove(pathElements.size()-1);
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }

    @Override
    public void updateProgress(String s) throws IOException {
        // not necessary yet
    }
}
