package de.unijena.bioinf.ms.projectspace;

import java.io.*;

public class SiriusFileWriter implements DirectoryWriter.WritingEnvironment {

    protected File root;
    protected BufferedWriter progressOutput = null;
    protected OutputStream currentStream = null;

    public SiriusFileWriter(File root) throws IOException {
        this.root = root;
        this.progressOutput = new BufferedWriter(new FileWriter(new File(root, ".progress")));
    }

    @Override
    public void enterDirectory(String name) {
        this.root = new File(root, name);
        root.mkdirs();
    }

    @Override
    public boolean deleteDirectory(String name) {
        return new File(root, name).delete();
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
    public void leaveDirectory() {
        root = root.getParentFile();
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
