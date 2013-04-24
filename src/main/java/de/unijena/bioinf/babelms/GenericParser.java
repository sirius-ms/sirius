package de.unijena.bioinf.babelms;

import java.io.*;

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

    public <S extends T> S parseFile(File file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            return parse(reader);
        } finally {
            if (reader != null) reader.close();
        }
    }
}
