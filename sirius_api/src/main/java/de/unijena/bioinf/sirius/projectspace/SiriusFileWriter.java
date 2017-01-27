package de.unijena.bioinf.sirius.projectspace;

import java.io.*;

public class SiriusFileWriter implements DirectoryWriter.WritingEnvironment {

    protected File root;
    protected OutputStream currentStream = null;

    @Override
    public void enterDirectory(String name) {
        this.root = new File(root, name);
        root.mkdirs();
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
    }
}
