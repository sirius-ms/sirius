/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
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

    public <S extends T> Iterator<S> parseIterator(final BufferedReader r) throws IOException {
        return new Iterator<S>() {
            BufferedReader reader=r;
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
                    reader=null;
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
