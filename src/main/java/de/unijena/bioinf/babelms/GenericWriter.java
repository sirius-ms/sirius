package de.unijena.bioinf.babelms;

import java.io.*;

/**
 * Created by kaidu on 12/5/13.
 */
public class GenericWriter<T> implements DataWriter<T> {

    private DataWriter<T> writer;

    public GenericWriter(DataWriter<T> writer) {
        this.writer = writer;
    }


    @Override
    public void write(BufferedWriter w, T obj) throws IOException {
        writer.write(w, obj);
    }

    public void write(OutputStream out, T obj) throws IOException {
        final BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out));
        write(w, obj);
    }

    public void writeToFile(File file, T obj) throws IOException {
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new FileWriter(file));
            write(w, obj);
        } finally {
            if (w != null) w.close();
        }
    }
}
