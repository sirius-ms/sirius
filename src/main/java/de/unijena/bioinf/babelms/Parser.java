package de.unijena.bioinf.babelms;

import java.io.BufferedReader;
import java.io.IOException;

public interface Parser<T> {

    public T parse(BufferedReader reader) throws IOException;

}
