package de.unijena.bioinf.babelms.projectspace;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.LinkedList;
import java.util.Queue;

public class SiriusFileWriter implements DirectoryWriter.WritingEnvironment {

    protected File root;
    protected Queue<File> realRoots = new LinkedList<>();

    protected BufferedWriter progressOutput = null;
    protected OutputStream currentStream = null;

    public SiriusFileWriter(File root) throws IOException {
        this.root = root;
        this.progressOutput = new BufferedWriter(new FileWriter(new File(root, ".progress")));
    }

    //todo i think we need a special override option here. To just create temp on experiment level
    @Override
    public void enterDirectory(String name, boolean rewriteTreeIfItExists) {
        final File toCreate = new File(root, name);
        if (!toCreate.mkdir() && rewriteTreeIfItExists) {
            root = de.unijena.bioinf.ChemistryBase.utils
                    .FileUtils.newTempFile(root.getPath(), ".", "-" + name).toFile();
            root.mkdirs();
                realRoots.add(toCreate);
        } else {
            root = new File(root, name);
        }
    }

    @Override
    public void leaveDirectory() {
        final File current = root;
        root = root.getParentFile();
        try {
            if (realRoots.peek() != null && realRoots.peek().getParentFile().equals(root)) {
                deleteDirectory(realRoots.peek().getName());
                FileUtils.moveDirectory(current, realRoots.poll());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDirectory(String name) throws IOException {
        FileUtils.deleteDirectory(new File(root.getAbsolutePath(), name));
    }


    @Override
    public OutputStream openFile(String name) {
        try {
            this.currentStream = new BufferedOutputStream(new FileOutputStream(new File(root, name)), 32768);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return currentStream;
    }

    @Override
    public void closeFile() {
        try {
            this.currentStream.close();
            this.currentStream = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (currentStream!=null) throw new IOException("Last file was not properly closed");
        progressOutput.close();
    }

    @Override
    public void updateProgress(String s) throws IOException {
        progressOutput.write(s);
        progressOutput.flush(); // always flush progressOutput
    }
}
