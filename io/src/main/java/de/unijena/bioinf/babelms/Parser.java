
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public interface Parser<T> {

    /**
     * Parses the next element lying in the given input stream. If the stream is empty, this
     * method returns null.
     * The implementation of Parser is free to assume that
     * for a given file or input source all elements are parsed consecutively with the same parser instance
     * So if the input contains a header the first call of parse might store this header for consecutive calls of
     * parse.
     * The implementation of parser must not assume that all entries in a data source are parsed
     *
     * @param reader input stream
     * @return data element from the input stream
     * @throws IOException if an IO error happens
     */
    T parse(BufferedReader reader, URI source) throws IOException;

    T parse(InputStream inputStream, URI source) throws IOException;
}
