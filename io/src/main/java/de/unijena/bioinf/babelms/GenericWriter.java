
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

import java.io.*;

/**
 * Created by kaidu on 12/5/13.
 */
public class GenericWriter<T> implements DataWriter<T> {

    private DataWriter<T> writer;

    public GenericWriter(DataWriter<T> writer) {
        this.writer = writer;
    }


    @Override
    public void write(BufferedWriter w, T obj) throws IOException {
        writer.write(w, obj);
    }

    public void write(OutputStream out, T obj) throws IOException {
        final BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out));
        write(w, obj);
    }

    public void writeToFile(File file, T obj) throws IOException {
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new FileWriter(file));
            write(w, obj);
        } finally {
            if (w != null) w.close();
        }
    }
}
