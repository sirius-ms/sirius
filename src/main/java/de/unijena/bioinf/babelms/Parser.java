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
