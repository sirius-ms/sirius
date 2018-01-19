package de.unijena.bioinf.sirius.logging;


import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class TextAreaHandler extends StreamHandler {
    private void configure() {
        setFormatter(new SimpleFormatter());
        try {
            setEncoding("UTF-8");
        } catch (IOException ex) {
            try {
                setEncoding(null);
            } catch (IOException ex2) {
                // doing a setEncoding with null should always work.
                // assert false;
                ex2.printStackTrace();
            }
        }
    }

    public TextAreaHandler(OutputStream os, Level level) {
        super();
        configure();
        setOutputStream(os);
        setLevel(level);
    }

    // [UnsynchronizedOverridesSynchronized] Unsynchronized method publish overrides synchronized method in StreamHandler
    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    @Override
    public synchronized void publish(LogRecord record) {
        super.publish(record);
        flush();
    }

    // [UnsynchronizedOverridesSynchronized] Unsynchronized method close overrides synchronized method in StreamHandler
    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    @Override
    public synchronized void close() {
        flush();
    }
}