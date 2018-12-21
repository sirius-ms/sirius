package de.unijena.bioinf.ms.projectspace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SiriusZipFileReader implements DirectoryReader.ReadingEnvironment {

    protected ZipFile zipFile;

    protected DirectoryTree directory;
    protected ArrayList<DirectoryTree> stack;
    protected InputStream currentStream = null;
    protected File root;

    public SiriusZipFileReader(File file) throws IOException {
        this.zipFile = new ZipFile(file, Charset.forName("UTF-8"));
        this.root = file;
        this.directory = new DirectoryTree("");
        this.stack = new ArrayList<>();
        stack.add(directory);
        makeIndex();
    }

    protected void makeIndex() {
        final Enumeration<? extends ZipEntry> zit = zipFile.entries();
        while (zit.hasMoreElements()) {
            final ZipEntry z = zit.nextElement();
            final String[] parts = z.getName().split("/");
            DirectoryTree node = directory;
            for (int k=0; k < parts.length; ++k) {
                if (parts[k].isEmpty()) continue;
                node = node.getOrAdd(parts[k]);
            }
        }
    }

    @Override
    public List<String> list() {
        return new ArrayList<>(stack.get(stack.size()-1).children.keySet());
    }

    @Override
    public void enterDirectory(String name) throws IOException {
        DirectoryTree tree = stack.get(stack.size()-1).children.get(name);
        if (tree == null) {
            throw new IOException("Unknown directory '" + join(stack, name) + "'");
        }
        stack.add(tree);
    }

    @Override
    public boolean isDirectory(String name) {
        DirectoryTree tree = stack.get(stack.size()-1).children.get(name);
        return tree!=null && tree.degree()>0;
    }

    private String join(List<DirectoryTree> stack, String name) {
        StringBuilder buf = new StringBuilder();
        for (DirectoryTree t : stack) {
            buf.append(t.name).append('/');
        }
        buf.append(name);
        return buf.toString();
    }

    @Override
    public InputStream openFile(String name) throws IOException {
        currentStream = zipFile.getInputStream(zipFile.getEntry(join(stack.subList(1,stack.size()), name)));
        return currentStream;
    }

    @Override
    public URL currentAbsolutePath(String name) throws IOException {
        final String path = stack.stream().map(tree -> tree.name).collect(Collectors.joining("/"));
        return new URL("jar:file:/" + root.getAbsolutePath() + "!/" + path + "/" + name);
    }

    @Override
    public URL absolutePath(String name) throws IOException {
        return new URL("jar:file:/" + root.getAbsolutePath() + "!/" + name);
    }

    @Override
    public void closeFile() throws IOException {
        currentStream.close();
    }

    @Override
    public void leaveDirectory() throws IOException {
        stack.remove(stack.size()-1);
    }

    @Override
    public void close() throws IOException {
        zipFile.close();
    }

    protected static class DirectoryTree {
        protected HashMap<String, DirectoryTree> children;
        protected static HashMap<String, DirectoryTree> EMPTY = new HashMap<>();
        protected String name;
        protected DirectoryTree(String name) {
            this.children = EMPTY;
            this.name = name;
        }

        protected int degree() {
            return children.size();
        }

        protected DirectoryTree addChild(String name) {
            if (children==EMPTY) {
                children = new HashMap<>();
            }
            DirectoryTree child = new DirectoryTree(name);
            children.put(name, child);
            return child;
        }

        protected DirectoryTree getOrAdd(String name) {
            DirectoryTree c = children.get(name);
            if (c==null) c = addChild(name);
            return c;
        }


    }

}
