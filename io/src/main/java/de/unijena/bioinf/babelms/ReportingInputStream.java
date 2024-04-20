/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms;

import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class ReportingInputStream extends InputStream implements Closeable {
    private final int reportingChunkSizeInBytes;
    @Getter
    private long totalBytesRead = 0;
    @Getter
    private int currentChunkRead = 0;
    private InputStream source;

    List<BiConsumer<Integer, Long>> listeners = new ArrayList<>();

    public ReportingInputStream(InputStream source) {
        this(source, 1024);
    }

    public ReportingInputStream(InputStream source, int reportingChunkSizeInBytes) {
        this.source = source;
        this.reportingChunkSizeInBytes = reportingChunkSizeInBytes;
    }


    @Override
    public int read() throws IOException {
        int result = source.read();
        if (result > 0) {
            currentChunkRead ++;
            totalBytesRead ++;
            if (currentChunkRead >= reportingChunkSizeInBytes) {
                listeners.forEach(l -> l.accept(currentChunkRead, totalBytesRead));
                currentChunkRead = 0;
            }
        } else if (currentChunkRead > 0){
            listeners.forEach(l -> l.accept(currentChunkRead, totalBytesRead));
            currentChunkRead = 0;
        }

        return result;
    }

    public synchronized void addBytesRaiseListener(BiConsumer<Integer, Long> listener) {
        listeners.add(listener);
    }

    public synchronized void removeBytesRaiseListener(BiConsumer<Integer, Long> listener) {
        listeners.remove(listener);
    }

    @Override
    public void close() throws IOException {
        super.close();
        source.close();
        if(currentChunkRead >0){
            listeners.forEach(l -> l.accept(currentChunkRead, totalBytesRead));
            currentChunkRead = 0;
        }
    }
}
