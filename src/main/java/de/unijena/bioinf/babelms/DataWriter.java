package de.unijena.bioinf.babelms;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Created by kaidu on 12/5/13.
 */
public interface DataWriter<T> {

    public void write(BufferedWriter writer, T data) throws IOException;

}
