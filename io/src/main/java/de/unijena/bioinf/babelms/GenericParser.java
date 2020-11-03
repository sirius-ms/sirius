
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GenericParser<T> implements Parser<T> {

    @NotNull private final Parser<T> parser;
    @NotNull private final Consumer<T> postProcessor;

    public GenericParser(Parser<T> parser) {
        this(parser, t -> {}); //default not postprocessing
    }

    public GenericParser(Parser<T> parser, @NotNull Consumer<T> postProcessor) {
        this.parser = parser;
        this.postProcessor = postProcessor;
    }


    @Deprecated
    public <S extends T> S parse(BufferedReader reader) throws IOException {
        return parse(reader, null);
    }

    public <S extends T> S parse(InputStream input) throws IOException {
        final BufferedReader reader = FileUtils.ensureBuffering(new InputStreamReader(input));
        return parse(reader);
    }

    @Deprecated
    public <S extends T> CloseableIterator<S> parseIterator(InputStream input) throws IOException {
        return parseIterator(input, null);
    }

    @Deprecated
    public <S extends T> CloseableIterator<S> parseIterator(BufferedReader input) throws IOException {
        return parseIterator(input, null);
    }

    public <S extends T> CloseableIterator<S> parseIterator(InputStream input, URL source) throws IOException {
        final BufferedReader reader = FileUtils.ensureBuffering(new InputStreamReader(input));
        return parseIterator(reader, source);
    }

    public <S extends T> CloseableIterator<S> parseIterator(final BufferedReader r, final URL source) throws IOException {
        return new CloseableIterator<S>() {
            @Override
            public void close() throws IOException {
                tryclose();
            }

            BufferedReader reader=r;
            S elem = parse(reader, source);
            @Override
            public boolean hasNext() {
                return reader != null;
            }

            @Override
            public S next() {
                S mem = elem;
                try {
                    if (parser.isClosingAfterParsing()) {
                        reader=null;
                        elem = null;
                        return mem;
                    }
                    elem = parse(reader, source);
                } catch (IOException e) {
                    tryclose();
                    throw new RuntimeException(e);
                }
                if (elem==null) tryclose();
                return mem;
            }

            private void tryclose() {
                try {
                    if (reader != null) {
                        reader.close();
                        reader=null;
                    }
                } catch (IOException e) {

                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public <S extends T> CloseableIterator<S> parseFromFileIterator(File file) throws IOException {
        final BufferedReader r = FileUtils.ensureBuffering(new FileReader(file));
        return parseIterator(r, file.toURI().toURL());
    }

    public <S extends T> CloseableIterator<S> parseFromPathIterator(Path file) throws IOException {
        final BufferedReader r = Files.newBufferedReader(file);
        return parseIterator(r, file.toUri().toURL());
    }


    public <S extends T> List<S> parseFromFile(File file) throws IOException {
        BufferedReader reader = null;
        final URL source = file.toURI().toURL();
        try {
            reader = FileUtils.ensureBuffering(new FileReader(file));
            final ArrayList<S> list = new ArrayList<S>();
            S elem = parse(reader,source);
            if (parser.isClosingAfterParsing()) {
                list.add(elem);
            } else {
                while (elem != null) {
                    list.add(elem);
                    elem = parse(reader, source);
                }
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
        final URL source = file.toURI().toURL();
        try {
            reader = FileUtils.ensureBuffering(new FileReader(file));
            return parse(reader,source);
        } catch (IOException e) {
            final IOException newOne = new IOException("Error while parsing " + file.getName(), e);
            throw newOne;
        } finally {
            if (reader != null) reader.close();
        }
    }

    @Override
    public <S extends T> S parse(BufferedReader reader, URL source) throws IOException {
        S it = parser.parse(reader, source);
        if (it != null)
            postProcessor.accept(it);
        return it;
    }
}
