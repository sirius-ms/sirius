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


package de.unijena.bioinf.storage.blob;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public abstract class AbstractCompressible implements Compressible {
   protected Compression compression;
   protected volatile boolean decompressStreams;

    protected AbstractCompressible(Compression compression) {
        this.compression = compression;
    }

    @Override
    public boolean isDecompressStreams() {
        return decompressStreams;
    }

    @Override
    public void setDecompressStreams(boolean decompress) {
        this.decompressStreams = decompress;
    }

    @Override
    public @NotNull Compression getCompression() {
        return compression;
    }

    protected Path addExt(Path relative){
        return Path.of(relative.toString() + getCompression().ext());
    }
}
