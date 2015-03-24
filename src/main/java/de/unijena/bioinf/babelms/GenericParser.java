package de.unijena.bioinf.babelms;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GenericParser<T> implements Parser<T> {

    private final Parser<T> parser;

    public GenericParser(Parser<T> parser) {
        this.parser = parser;
    }


    @Override
    public <S extends T> S parse(BufferedReader reader) throws IOException {
        return parser.parse(reader);
    }

    public <S extends T> S parse(InputStream input) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        return parse(reader);
    }

    public <S extends T> Iterator<S> parseIterator(InputStream input) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        return parseIterator(reader);
    }

    public <S extends T> Iterator<S> parseIterator(final BufferedReader reader) throws IOException {
        return new Iterator<S>() {
            S elem = parse(reader);
            @Override
            public boolean hasNext() {
                return reader != null;
            }

            @Override
            public S next() {
                S mem = elem;
                try {
                    elem = parse(reader);
                } catch (IOException e) {
                    tryclose();
                    throw new RuntimeException(e);
                }
                if (elem==null) tryclose();
                return mem;
            }

            private void tryclose() {
                try {
                    reader.close();
                } catch (IOException e) {

                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public <S extends T> Iterator<S> parseFromFileIterator(File file) throws IOException {
        final BufferedReader r = new BufferedReader(new FileReader(file));
        return parseIterator(r);
    }


    public <S extends T> List<S> parseFromFile(File file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            final ArrayList<S> list = new ArrayList<S>();
            S elem = parse(reader);
            while (elem!=null) {
                list.add(elem);
                elem = parse(reader);
            }
            return list;
        } catch (IOException e) {
            final IOException newOne = new IOException("Error while parsing " + file.getName(), e);
            throw newOne;
        } finally {
            if (reader != null) reader.close();
        }
    }

    @Deprecated
    public <S extends T> S parseFile(File file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            return parse(reader);
        } catch (IOException e) {
            final IOException newOne = new IOException("Error while parsing " + file.getName(), e);
            throw newOne;
        } finally {
            if (reader != null) reader.close();
        }
    }
}
