package de.unijena.bioinf.babelms;

import java.io.BufferedReader;
import java.io.IOException;

public interface Parser<T> {

    /**
     * parses the next element lying in the given input stream. If the stream is empty, this
     * method returns null
     *
     * The implementation of Parser is free to assume that
     * for a given file or input source all elements are parsed consecutively with the same parser instance
     * So if the input contains a header the first call of parse might store this header for consecutive calls of
     * parse.
     * The implementation of parser must not assume that all entries in a data source are parsed
     *
     * @param reader input stream
     * @param <S> data type that is parsed from the input stream
     * @return data element from the input stream
     * @throws IOException
     */
    public <S extends T> S parse(BufferedReader reader) throws IOException;

}
