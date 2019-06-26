package de.unijena.bioinf.babelms.projectspace;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SiriusFileReader implements DirectoryReader.ReadingEnvironment {

    protected File root, current;
    protected InputStream currentStream;

    public SiriusFileReader(File root) {
        if (root == null) throw new NullPointerException();
        this.root = root;
        this.current = root;
    }

    @Override
    public List<String> list() {
        final String[] alist = current.list();
        if (alist==null) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(alist));
    }

    @Override
    public void enterDirectory(String name) throws IOException {
        current = new File(current, name);
        if (!current.exists()) throw new IOException("Unknown directory '" + current + "'");
    }

    @Override
    public boolean isDirectory(String name) {
        File file = new File(current, name);
        return file.isDirectory();
    }

    @Override
    public InputStream openFile(String name) throws IOException {
        currentStream = new FileInputStream(new File(current, name));
        return currentStream;
    }

    @Override
    public boolean containsFile(@NotNull String fileName) {
        return new File(current, fileName).isFile();
    }

    @Override
    public URL currentAbsolutePath(String name) throws IOException {
        return new File(current, name).toURI().toURL();
    }

    @Override
    public URL absolutePath(String name) throws IOException {
        return new File(root, name).toURI().toURL();
    }

    @Override
    public void closeFile() throws IOException {
        currentStream.close();
        currentStream = null;
    }

    @Override
    public void leaveDirectory() throws IOException {
        current = current.getParentFile();
    }

    @Override
    public void close() throws IOException {

    }
}
