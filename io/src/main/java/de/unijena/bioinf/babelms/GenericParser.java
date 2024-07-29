
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
import java.net.URI;
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

    public GenericParser(@NotNull Parser<T> parser, @NotNull Consumer<T> postProcessor) {
        this.parser = parser;
        this.postProcessor = postProcessor;
    }


    @Deprecated
    public T parse(BufferedReader reader) throws IOException {
        return parse(reader, null);
    }

    public T parse(InputStream input) throws IOException {
        return parse(input, null);
    }

    @Deprecated
    public CloseableIterator<T> parseIterator(InputStream input) throws IOException {
        return parseIterator(input, null);
    }

    @Deprecated
    public CloseableIterator<T> parseIterator(BufferedReader input) throws IOException {
        return parseIterator(input, null);
    }

    public CloseableIterator<T> parseIterator(InputStream input, URI source) throws IOException {
        return new CloseableIterator<>() {
            @Override
            public void close() {
                tryclose();
            }

            InputStream reader = input;
            T nextElement = parse(reader, source);

            @Override
            public boolean hasNext() {
                if (nextElement == null) tryclose(); //for reader without any element
                return reader != null;
            }

            @Override
            public T next() {
                T current = nextElement;
                try {
                    nextElement = parse(reader, source);
                } catch (IOException e) {
                    tryclose();
                    throw new RuntimeException(e);
                }
                if (nextElement == null) tryclose();
                return current;
            }

            private void tryclose() {
                try {
                    if (reader != null) {
                        reader.close();
                        reader = null;
                    }
                } catch (IOException ignored) {}
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public CloseableIterator<T> parseIterator(final BufferedReader r, final URI source) throws IOException {
        return new CloseableIterator<>() {
            @Override
            public void close() {
                tryclose();
            }

            BufferedReader reader = r;
            T nextElement = parse(reader, source);

            @Override
            public boolean hasNext() {
                if (nextElement == null) tryclose(); //for reader without any element
                return reader != null;
            }

            @Override
            public T next() {
                T current = nextElement;
                try {
                    nextElement = parse(reader, source);
                } catch (IOException e) {
                    tryclose();
                    throw new RuntimeException(e);
                }
                if (nextElement == null) tryclose();
                return current;
            }

            private void tryclose() {
                try {
                    if (reader != null) {
                        reader.close();
                        reader = null;
                    }
                } catch (IOException ignored) {}
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public CloseableIterator<T> parseFromFileIterator(File file) throws IOException {
        final BufferedReader r = FileUtils.ensureBuffering(new FileReader(file));
        return parseIterator(r, file.toURI());
    }

    public CloseableIterator<T> parseFromPathIterator(Path file) throws IOException {
        final BufferedReader r = Files.newBufferedReader(file);
        return parseIterator(r, file.toUri());
    }


    public List<T> parseFromFile(File file) throws IOException {
        final URI source = file.toURI();
        try (BufferedReader reader = FileUtils.ensureBuffering(new FileReader(file))) {
            List<T> list = new ArrayList<>();
            parseIterator(reader, source).forEachRemaining(list::add);
            return list;
        } catch (IOException e) {
            throw new IOException("Error while parsing " + file.getName(), e);
        }
    }

    @Deprecated
    public T parseFile(File file) throws IOException {
        BufferedReader reader = null;
        final URI source = file.toURI();
        try {
            reader = FileUtils.ensureBuffering(new FileReader(file));
            return parse(reader, source);
        } catch (IOException e) {
            throw new IOException("Error while parsing " + file.getName(), e);
        } finally {
            if (reader != null) reader.close();
        }
    }

    @Override
    public T parse(BufferedReader reader, URI source) throws IOException {
        T it = parser.parse(reader, source);
        if (it != null)
            postProcessor.accept(it);
        return it;
    }

    @Override
    public T parse(InputStream stream, URI source) throws IOException {
        T it = parser.parse(stream, source);
        if (it != null)
            postProcessor.accept(it);
        return it;
    }
}
