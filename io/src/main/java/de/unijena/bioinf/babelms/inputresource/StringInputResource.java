/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.inputresource;

import java.io.*;
import java.net.URI;

public class StringInputResource implements InputResource<String> {

    private final String data;
    private final String name;
    private final String ext;

    public StringInputResource(String data, String name, String ext) {
        this.data = data;
        this.name = name;
        this.ext = ext;
    }

    @Override
    public String getResource() {
        return data;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return data.getBytes();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(getBytes());
    }

    @Override
    public BufferedReader getBufferedReader() throws IOException {
        return new BufferedReader(new StringReader(getResource()));
    }

    @Override
    public String getFilename() {
        return name + ext;
    }

    @Override
    public String getFileExt() {
        return ext;
    }

    @Override
    public URI toUri() {
        return URI.create(getFilename());
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public long getSize() {
        return data.length();
    }
}
