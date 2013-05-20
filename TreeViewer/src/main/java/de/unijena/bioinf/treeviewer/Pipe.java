package de.unijena.bioinf.treeviewer;


import java.io.*;
import java.util.logging.Logger;

public class Pipe extends IOConnection implements Runnable {

    private final BufferedReader reader;
    public Pipe(InputStream stream, FileListPane pane) {
        super(pane);
        this.reader = new BufferedReader(new InputStreamReader(stream));
        new Thread(this).start();
    }

    @Override
    public void run() {
        readInput(reader);
    }
}
