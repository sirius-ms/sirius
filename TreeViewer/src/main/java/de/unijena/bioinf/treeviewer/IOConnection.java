package de.unijena.bioinf.treeviewer;

import java.io.*;
import java.util.logging.Logger;

public class IOConnection {

    public static String FILE = "<!file>", DOT = "<!dot>", ACTIVATE = "<!enableLast>", FLUSH = "<!flush>";


    private static Logger logger = Logger.getLogger(Pipe.class.getSimpleName());

    private final FileListPane pane;
    private boolean closed;
    private int state = 0;
    private StringBuilder buffer;

    public IOConnection(FileListPane pane) {
        this.pane = pane;
        this.buffer = new StringBuilder();
        this.closed = false;
    }

    public void close() {
        this.closed = true;
    }

    public void readInput(BufferedReader reader) {
        try {
            while (!closed) {
                final String line = reader.readLine();
                if (line == null) break;
                if (line.indexOf('\000') >= 0) break;
                if (line.startsWith("<!")) {
                    if (line.equals(FILE)) {
                        flush();
                        state = 1;
                    } else if (line.equals(DOT)) {
                        flush();
                        state = 2;
                    } else if (line.equals(ACTIVATE)) {
                        flush();
                        pane.enableLast();
                        state=0;
                        buffer.delete(0, buffer.length());
                    } else if (line.equals(FLUSH)) {
                        flush();
                        state=0;
                        buffer.delete(0, buffer.length());
                    } else {
                        logger.warning("Unknown control sequenz: '" + line + "'");
                    }
                } else {
                    buffer.append(line).append("\n");
                }
            }
            if (closed) logger.info("close input stream");
            else logger.info("end of file reached");
        } catch (IOException e) {
            logger.severe(e.getMessage());
        } finally {
            flush();
            try {
                reader.close();
            } catch (IOException e) {
                logger.severe(e.getMessage());
            }
        }
    }

    private void flush() {
        if (state == 1) {
            // read from file
            final File file = new File(buffer.toString().trim());
            if (file.exists()) {
                pane.addFileOrDirectory(file);
            } else {
                logger.warning("file does not exist: '" + file.getPath() + "'");
            }
        } else if (state == 2) {
            // read from input
            final String[] str = buffer.toString().split("\n", 2);
            final DotMemory memory = new DotMemory(str[0], str[1]);
            //logger.info(str[0]);
            pane.addFile(memory);
        }
        state = 0;
    }

}
